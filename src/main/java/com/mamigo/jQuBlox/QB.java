package com.mamigo.jQuBlox;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import us.monoid.json.JSONObject;
import us.monoid.web.BinaryResource;
import us.monoid.web.FormData;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.mime.MultipartContent;

public class QB {
	class QBHeaders{
		public static final String VERSION="QuickBlox-REST-API-Version";
		public static final String ACCKEY="QB-Account-Key";
		public static final String TOKEN="QB-Token";
	}
	
	private String apiversion="0.1.1";
	private String token=null;
	private String apiep="https://api.quickblox.com";
	private Date tokenUpdatedTime=null;
	private String s3_bucket_name="qbprod"; 
	
	public Date getTokenUpdatedTime() {
		return tokenUpdatedTime;
	}

	
	private String makeURI(String resource) throws Exception{
		if(apiep==null) throw new Exception("QB Client not connected yet");
		return apiep+"/"+resource;
	}
	
	private Resty makeResty() {
		Resty r = new Resty();
		r.withHeader(QBHeaders.VERSION, apiversion);
		if(token!=null)
			r.withHeader(QBHeaders.TOKEN, token);
		return r;
	}


	private String makeSignature(SortedMap<String,String> kvPairs, String auth_secret) throws Exception
	{
		StringBuilder sb = new StringBuilder();
		for(String k:kvPairs.keySet())
		{
			sb.append(k).append('=').append(kvPairs.get(k)).append('&');
		}
		if(kvPairs.size()>0)
		{
			sb.deleteCharAt(sb.length()-1); //remove the last &
		}
		
		return sha256_HMAC_encode(auth_secret,sb.toString());
		
	}
	
	public static String sha256_HMAC_encode(String key, String data) throws Exception {
		final Charset asciiCs = Charset.forName("US-ASCII");
		  Mac sha256_HMAC = Mac.getInstance("HmacSHA1");
		  SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(asciiCs), "HmacSHA1");
		  sha256_HMAC.init(secret_key);
		  return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes(asciiCs)));
		}
	
	
	
	public String getAPIEndPoint(){ return apiep;} 
	
	public void connect(String accountKey) throws Exception {
		Resty r = makeResty();
		r.withHeader(QBHeaders.ACCKEY,accountKey);
		JSONResource resp = r.json("https://api.quickblox.com/account_settings.json");
		apiep=resp.get("api_endpoint").toString();
		s3_bucket_name=resp.get("s3_bucket_name").toString();
	}

	
	public void session(String application_id, String auth_key, String auth_secret) throws Exception {
		SortedMap<String,String> kvPairs = new TreeMap<String, String>();
		kvPairs.put("application_id", application_id);
		kvPairs.put("auth_key", auth_key);
		kvPairs.put("timestamp", ""+(System.currentTimeMillis() / 1000L));
		kvPairs.put("nonce",  ""+(int)(Math.random()*10000));
		String signature=makeSignature(kvPairs,auth_secret);
		kvPairs.put("signature",  signature);
		JSONObject req= new JSONObject(kvPairs);
		
		Resty r = makeResty();
		JSONResource resp =r.json(makeURI("session.json"),Resty.content(req));
		token=resp.get("session.token").toString();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-DD'T'HH:mm:ssX");
		tokenUpdatedTime=sdf.parse(resp.get("session.updated_at").toString());
		
	}
	public void login(String user_id, String email, String password) throws Exception {
		JSONObject req = new JSONObject();
		if(user_id!=null) req.append("login", user_id);
		if(email!=null) req.append("email", email);
		req.append("password", password);
		Resty r = makeResty();
		JSONResource resp =r.json(makeURI("login.json"),Resty.content(req));
		resp.get("user.full_name");
	}
	public File downloadContent(String id, String toFilePath) throws Exception
	{
		Resty r= makeResty();
		BinaryResource br= r.bytes(makeURI("blobs/"+id+".json"));
		return br.save(new File(toFilePath));
	}
	public boolean uploadContent(Blob b) throws Exception
	{
		//create file
		{
			JSONObject req = b.asJSon();
			Resty r= makeResty();
			JSONResource resp =r.json(makeURI("blobs.json"),Resty.content(req));
			b.setRemoteBlob(resp.object());
		}

		//upload file
		try
		{
			LinkedHashMap<String,String> params = new LinkedHashMap<String, String>();
			String url = b.getUploadParams(params);
			List<FormData> fdataList = new ArrayList<FormData>();
			for(Entry<String,String> kv:params.entrySet())
			{
				fdataList.add(Resty.data(kv.getKey(), kv.getValue()));
			}
			fdataList.add(new FormData("file", b.getName(), b.getLocalFileAsContent() ));
			MultipartContent mc= Resty.form(fdataList.toArray(new FormData[0]));
			Resty r= new Resty();
			b.setAwsFileInfo(r.xml(url, mc));
			//declare uploaded
			makeResty().json(makeURI("blobs/"+b.getID()+"/complete.json"), Resty.content(b.asSizeJson()));
		}
		catch(Throwable th)
		{
			System.out.println("could not upload a file to S3, reverting from QB due to:"+th.getMessage());
			deleteFile(b.getID());
			return false;
		}
		return true;
		
	}


	public void deleteFile(String id) throws IOException, Exception {
		Resty r =makeResty();
		r.json(makeURI("blobs/"+id+".json"), Resty.delete());
		
	}
}

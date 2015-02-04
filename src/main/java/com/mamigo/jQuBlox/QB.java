package com.mamigo.jQuBlox;

import static us.monoid.web.Resty.content;
import static us.monoid.web.Resty.data;
import static us.monoid.web.Resty.form;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import us.monoid.json.JSONObject;
import us.monoid.web.BinaryResource;
import us.monoid.web.FormData;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;
import us.monoid.web.mime.MultipartContent;

import com.qb.gson.Gson;
import com.qb.gson.GsonBuilder;
import com.quickblox.core.LogLevel;
import com.quickblox.core.QBCallback;
import com.quickblox.core.QBSettings;
import com.quickblox.core.result.Result;
import com.quickblox.internal.core.helper.StringifyArrayList;
import com.quickblox.internal.module.content.deserializer.QBFileStatusDeserializer;
import com.quickblox.internal.module.custom.request.QBCustomObjectRequestBuilder;
import com.quickblox.internal.module.custom.request.QBCustomObjectUpdateBuilder;
import com.quickblox.module.auth.QBAuth;
import com.quickblox.module.auth.model.QBSession;
import com.quickblox.module.auth.model.QBSessionWrap;
import com.quickblox.module.auth.result.QBSessionResult;
import com.quickblox.module.content.model.QBFile;
import com.quickblox.module.content.model.QBFileWrap;
import com.quickblox.module.custom.QBCustomObjects;
import com.quickblox.module.custom.model.QBCustomObject;
import com.quickblox.module.custom.model.QBCustomObjectDeleted;
import com.quickblox.module.custom.model.QBCustomObjectIds;
import com.quickblox.module.custom.result.QBCustomObjectCountResult;
import com.quickblox.module.custom.result.QBCustomObjectDeletedResult;
import com.quickblox.module.custom.result.QBCustomObjectLimitedResult;
import com.quickblox.module.custom.result.QBCustomObjectMultiUpdatedResult;
import com.quickblox.module.custom.result.QBCustomObjectResult;
import com.quickblox.module.messages.model.QBFileStatus;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.module.users.model.QBUserWrap;

public class QB {
	class QBHeaders{
		public static final String VERSION="QuickBlox-REST-API-Version";
		public static final String ACCKEY="QB-Account-Key";
		public static final String TOKEN="QB-Token";
	}
	private String apiversion="0.1.1";
	private String token=null;
	private Date tokenUpdatedTime=null;
	
	public QB(){
		QBSettings.getInstance().setSynchronous(true);
	}
	
	public Date getTokenUpdatedTime() {
		return tokenUpdatedTime;
	}
	public boolean isSessionRequired(){
		if(tokenUpdatedTime==null) return true;
		long OneHr58Min=2*59*60*1000;
		if(System.currentTimeMillis()-tokenUpdatedTime.getTime()>OneHr58Min)
			return true;
		
		return false;
	}
	
	private String makeURI(String resource) throws Exception{
		String apiep="https://"+QBSettings.getInstance().getServerApiDomain();
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
	
	
	
	public String getAPIEndPoint(){ return QBSettings.getInstance().getServerApiDomain();} 
	
	public void connect(String accountKey) throws Exception {
		Resty r = makeResty();
		r.withHeader(QBHeaders.ACCKEY,accountKey);
		JSONResource resp = r.json("https://api.quickblox.com/account_settings.json");
		
		QBSettings.getInstance().setLogLevel(LogLevel.DEBUG);
		
		QBSettings.getInstance()
//			.setServerApiDomain(resp.get("api_endpoint").toString())
			.setContentBucketName(resp.get("s3_bucket_name").toString());
	}

	
	public QBSession session(String application_id, String auth_key, String auth_secret) throws Exception {
		QBSettings qs = QBSettings.getInstance();
		qs.fastConfigInit(application_id, auth_key, auth_secret);
		return session();
	}
	public QBSession session() throws Exception {
		
		QBSessionWrap sessionHolder= new QBSessionWrap();
		QBAuth.createSession(new QBCallback() {
			@Override
			public void onComplete(Result result, Object obj) {
				((QBSessionWrap)obj).setSession(((QBSessionResult)result).getSession());
			}
			
			@Override
			public void onComplete(Result result) {
				
			}
		},sessionHolder);
		
//		QBSettings qs = QBSettings.getInstance();
//		SortedMap<String,String> kvPairs = new TreeMap<String, String>();
//		kvPairs.put("application_id", qs.getApplicationId());
//		kvPairs.put("auth_key", qs.getAuthorizationKey());
//		kvPairs.put("timestamp", ""+(System.currentTimeMillis() / 1000L));
//		kvPairs.put("nonce",  ""+(int)(Math.random()*10000));
//		String signature=makeSignature(kvPairs,qs.getAuthorizationSecret());
//		kvPairs.put("signature",  signature);
//		QBSession session=getQBObject(new JSONObject(kvPairs), "session.json", QBSessionWrap.class).getSession();
		QBSession session=sessionHolder.getSession();
		if(session!=null)
		{
	        token=session.getToken();
			tokenUpdatedTime=session.getUpdatedAt();
		}
		return session;
	}


	private <T> T getQBObject(JSONObject req, String resourceName, Class<T> c)throws IOException, Exception {
		Resty r = makeResty();
		String stringToParse =r.text(makeURI(resourceName),content(req)).toString();
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        return gson.fromJson(stringToParse, c);
	}
	//gsonBuilder.registerTypeAdapter(com/quickblox/module/messages/model/QBFileStatus, new QBFileStatusDeserializer());
	private <T,R> T getQBObject(JSONObject req, String resourceName, Class<T> objClass, Class<R> extraType, Object extraTypeAdapter)throws IOException, Exception {
		Resty r = makeResty();
		String stringToParse=null;
		if(req==null)
		{
			stringToParse =r.text(makeURI(resourceName)).toString();
		}
		else
		{
			stringToParse =r.text(makeURI(resourceName),content(req)).toString();
		}
        GsonBuilder gsonBuilder = new GsonBuilder();
        if(extraTypeAdapter!=null)
        	gsonBuilder.registerTypeAdapter(extraType, extraTypeAdapter);
        Gson gson = gsonBuilder.create();
        return gson.fromJson(stringToParse, objClass);
	}

	public QBUser login(String user_id, String email, String password) throws Exception {
		
		QBUser quser = new QBUser(user_id, password, email);
		
		JSONObject req = new JSONObject();
		if(user_id!=null) req.append("login", quser.getLogin());
		if(email!=null) req.append("email", quser.getEmail());
		req.append("password", quser.getPassword());
		return getQBObject(req, "login.json", QBUserWrap.class).getUser();
	}
	
//Content
	public File downloadContentByID(String id, String toFilePath) throws Exception
	{
		QBFile file = getQBObject(null,"blobs/"+id+".json",QBFileWrap.class,QBFileStatus.class,new QBFileStatusDeserializer()).getFile();
		if(file==null) return null;
		return downloadContentByUID(file.getUid(), toFilePath);
	}
	public File downloadContentByUID(String uid, String toFilePath) throws Exception
	{
		Resty r= makeResty();
		BinaryResource br= r.bytes(makeURI("blobs/"+uid+".json"));
		return br.save(new File(toFilePath));
	}
	public QBFile uploadContent(Blob b) throws Exception
	{
		//create file
		QBFile file = getQBObject(b.asJSon(),"blobs.json",QBFileWrap.class,QBFileStatus.class,new QBFileStatusDeserializer()).getFile();
		b.setRemoteBlob(file.getFileObjectAccess());

		//upload file
		try
		{
			LinkedHashMap<String,String> params = new LinkedHashMap<String, String>();
			String url = b.getUploadParams(params);
			List<FormData> fdataList = new ArrayList<FormData>();
			for(Entry<String,String> kv:params.entrySet())
			{
				fdataList.add(data(kv.getKey(), kv.getValue()));
			}
			fdataList.add(new FormData("file", b.getName(), b.getLocalFileAsContent() ));
			MultipartContent mc= form(fdataList.toArray(new FormData[0]));
			Resty r= new Resty();
			b.setAwsFileInfo(r.xml(url, mc));
			//declare uploaded
			makeResty().json(makeURI("blobs/"+file.getId()+"/complete.json"), content(b.asSizeJson()));
		}
		catch(Throwable th)
		{
			System.out.println("could not upload a file to S3, reverting from QB due to:"+th.getMessage());
			deleteFile(file.getId());
			return null;
		}
		return file;
		
	}


	public void deleteFile(int id) throws IOException, Exception {
		Resty r =makeResty();
		r.json(makeURI("blobs/"+id+".json"), Resty.delete());
		
	}

//Custom Objects	
	public boolean createObject(QBCustomObject customObject) throws Exception
	{
		CustomObjectCB cb =new CustomObjectCB();
		QBCustomObjects.createObject(customObject, cb);
		if(cb.obj==null) return false;
		cb.obj.copyFieldsTo(customObject);
		return true;
	}
	public boolean updateObject(QBCustomObject customObject) throws Exception
	{
		CustomObjectCB cb =new CustomObjectCB();
		QBCustomObjects.updateObject(customObject, cb);
		if(cb.obj==null) return false;
		cb.obj.copyFieldsTo(customObject);
		return true;
	}
	
	public boolean incrementFieldBy(QBCustomObject customObject, String fieldName, Number value) throws Exception
	{
		QBCustomObjectUpdateBuilder builder = new QBCustomObjectUpdateBuilder();
		builder.inc(fieldName, value);
		
		CustomObjectCB cb =new CustomObjectCB();
		QBCustomObjects.updateObject(customObject,builder,cb);
		if(cb.obj==null) return false;
		cb.obj.copyFieldsTo(customObject);
		return true;
	}
	public QBCustomObjectMultiUpdatedResult updateObjects(List<QBCustomObject> customObjects) throws Exception
	{
		CustomObjectMultiCB cb =new CustomObjectMultiCB();
		QBCustomObjects.updateObjects(customObjects, cb);
		return cb.obj;
	}
	public boolean deleteObject(QBCustomObject customObject) throws Exception
	{
		DeleteCB dcb = new DeleteCB();
		QBCustomObjects.deleteObject(customObject, dcb);
		return dcb.deletedSingle;
	}
	public QBCustomObjectDeleted deleteObjects(String classname, List<String> ids) throws Exception
	{
		DeleteCB dcb = new DeleteCB();
		QBCustomObjects.deleteObjects(classname, new StringifyArrayList<String>(ids), dcb);
		return dcb.deleted;
	}

	private static boolean checkErrors(Result r){
		if(r.isSuccess()) return true;
		for(String s:r.getErrors()) System.out.println(s);
		return false;
	}
	private final class CountCB implements QBCallback {
		int count=0;
		@Override
		public void onComplete(Result arg0, Object c) {
			onComplete(arg0);
		}

		@Override
		public void onComplete(Result arg0) {
			if(!checkErrors(arg0)) return;
			count=((QBCustomObjectCountResult)arg0).getCount();
		}
	}
	private final class DeleteCB implements QBCallback {
		QBCustomObjectDeleted deleted=new QBCustomObjectDeleted();
		boolean deletedSingle=false;
		@Override
		public void onComplete(Result arg0, Object c) {
			onComplete(arg0);
		}

		@Override
		public void onComplete(Result arg0) {
			if(!checkErrors(arg0)) return;
			if(arg0 instanceof QBCustomObjectDeletedResult)
			{
				QBCustomObjectDeletedResult r = (QBCustomObjectDeletedResult)arg0;
				deleted.setDeleted(makeQBCustomObjectIds(r.getDeleted()));
				deleted.setNotFound(makeQBCustomObjectIds(r.getNotFound()));
				deleted.setWrongPermissions(makeQBCustomObjectIds(r.getWrongPermissions()));
			}
			else
			{
				deletedSingle=arg0.isSuccess();
			}
		}
	}
	private final class FetchMultiCB implements QBCallback{

		List<QBCustomObject> customObjects;

		@Override
		public void onComplete(Result result) {
			if(!checkErrors(result)) return;
			QBCustomObjectLimitedResult r = (QBCustomObjectLimitedResult)result;
			customObjects = r.getCustomObjects();
		}

		@Override
		public void onComplete(Result result, Object obj) {
			onComplete(result);
			
		}
		
	}

	private final class CustomObjectCB implements QBCallback {
		QBCustomObject obj=null;
		@Override
		public void onComplete(Result arg0, Object c) {
			onComplete(arg0);
		}

		@Override
		public void onComplete(Result arg0) {
			if(!checkErrors(arg0)) return;
			QBCustomObjectResult r = (QBCustomObjectResult)arg0;
			obj=r.getCustomObject();
		}
	}
	
	private final class CustomObjectMultiCB implements QBCallback {
		QBCustomObjectMultiUpdatedResult obj=null;
		@Override
		public void onComplete(Result arg0, Object c) {
			onComplete(arg0);
		}

		@Override
		public void onComplete(Result arg0) {
			if(!checkErrors(arg0)) return;
			obj = (QBCustomObjectMultiUpdatedResult)arg0;
		}
	}
	
	private static QBCustomObjectIds makeQBCustomObjectIds(ArrayList ids){
		QBCustomObjectIds ret = new QBCustomObjectIds();
		ret.setIds(ids);
		return ret;
	}


	public int countObjects(String className)
	{
		CountCB callback = new CountCB();
		QBCustomObjects.countObjects(className, callback);
		return callback.count;
	}
	
	public List<QBCustomObject> getAllObjects(String className)
	{
		FetchMultiCB cb = new FetchMultiCB();
		QBCustomObjects.getObjects(className, cb);
		return cb.customObjects;
	}
	
	public QBCustomObject getObject(String className, String customObjectId)
	{
		CustomObjectCB cb = new CustomObjectCB();
		QBCustomObjects.getObject(className, customObjectId, cb);
		return cb.obj;
	}
	
	public List<QBCustomObject> getObjects(String className,QBCustomObjectRequestBuilder builder)
	{
		FetchMultiCB cb = new FetchMultiCB();
		QBCustomObjects.getObjects(className,builder, cb);
		return cb.customObjects;
	}

}

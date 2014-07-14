package com.mamigo.jQuBlox;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Content;
import us.monoid.web.XMLResource;

import com.mamigo.util.CollectionUtils;

/**
 * 
 * Class to define a blob
 * @author akhil
 *
 */
public class Blob {
	public Blob(String contentType, String remoteName, String fileName) {
		super();
		this.contentType = contentType;
		this.name = remoteName;
		this.fileName=fileName;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	private String contentType;
	private String name;
	private String fileName;
	private int fileSize;
	
	private boolean isPublic;
	List<String> tags= new ArrayList<String>();
	
	private JSONObject remoteBlob=null;
	private XMLResource awsFileInfo=null;

	public XMLResource getAwsFileInfo() {
		return awsFileInfo;
	}
	public void setAwsFileInfo(XMLResource awsFileInfo) {
		this.awsFileInfo = awsFileInfo;
	}
	public JSONObject getRemoteBlob() {
		return remoteBlob;
	}
	public void setRemoteBlob(JSONObject remoteBlob) {
		this.remoteBlob = remoteBlob;
	}
	public JSONObject asJSon() throws JSONException
	{
		JSONObject b= new JSONObject();
		b.put("content_type", contentType);
		b.put("name", name);
		b.put("public", (isPublic)?"true":"false");
		if(!tags.isEmpty()) 
			b.put("tag_list", CollectionUtils.join(",", tags));
		return new JSONObject().put("blob", b);
	}
	public JSONObject asSizeJson() throws JSONException
	{
		JSONObject b= new JSONObject();
		b.put("size", ""+fileSize);
		return new JSONObject().put("blob", b);
	}
	public String getUploadParams(Map<String,String> ret) throws JSONException, UnsupportedEncodingException
	{
		
		//"params": "http://qbprod.s3.amazonaws.com/?AWSAccessKeyId=AKIAIY7KFM23XGXJ7R7A&Policy=eyAiZXhwaXJhdGlvbiI6ICIyMDEyLTA0LTIzVDE0OjIyOjM0WiIsCiAgICAgICJjb25kaXRpb25zIjogWwogICAgICAgIHsiYnVja2V0IjogInFicHJvZCJ9LAogICAgICAgIFsiZXEiLCAiJGtleSIsICIzMGE4YmNkN2M3MTQ0MTdlYjYyYjk1MzUwZDdlMTNiOTAwIl0sCiAgICAgICAgeyJhY2wiOiAiYXV0aGVudGljYXRlZC1yZWFkIn0sCiAgICAgICAgWyJlcSIsICIkQ29udGVudC1UeXBlIiwgImltYWdlL2pwZWciXSwKICAgICAgICB7InN1Y2Nlc3NfYWN0aW9uX3N0YXR1cyI6ICIyMDEifQogICAgICBdCiAgICB9&Signature=eBtgK1jAzsGNcFjpqEGiTLnm008%3D&key=30a8bcd7c714417eb62b95350d7e13b900&Content-Type=image%2Fjpeg&acl=authenticated-read&success_action_status=201"
		String params=remoteBlob.getJSONObject("blob").getJSONObject("blob_object_access").get("params").toString();
		System.out.println(params);
		StringTokenizer st = new StringTokenizer(params,"?&=");
		String url=st.nextToken();
		while(st.hasMoreTokens())
		{
			String k = st.nextToken();
			String v = st.nextToken();
			ret.put(k,URLDecoder.decode(v, "UTF-8"));
			System.out.println(k+"="+v);
		}
		return url;
	}
	public List<String> getUploadParams() throws JSONException
	{
		List<String> ret = new ArrayList<String>();
		String params=remoteBlob.getJSONObject("blob").getJSONObject("blob_object_access").get("params").toString();
		System.out.println(params);
		StringTokenizer st = new StringTokenizer(params,"?");
		ret.add(st.nextToken());
		ret.add(st.nextToken());
		return ret;
	}
	public Content getLocalFileAsContent() throws IOException
	{
		byte[] bytes=FileUtils.readFileToByteArray(new File(fileName));
		fileSize=bytes.length;
		return new Content(getContentType(),bytes);
	}
	public String getID() throws JSONException
	{
		return remoteBlob.getJSONObject("blob").getString("id");
	}
	
	public String getUID() throws JSONException
	{
		return remoteBlob.getJSONObject("blob").getString("uid");
	}

	
	public void makeCurlCmd(Map<String,String> params, String url)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("curl -X POST ");
		for(String k: params.keySet())
		{
			sb.append(" -F ").append("\"").append(k).append("=").append(params.get(k)).append("\"");
		}
		sb.append(" -F \"file=@Bear.bmp\" ");
		sb.append(url);
		System.out.println(sb.toString());
	}
}

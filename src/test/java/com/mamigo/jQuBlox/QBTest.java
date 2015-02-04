package com.mamigo.jQuBlox;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.quickblox.internal.module.custom.request.QBCustomObjectRequestBuilder;
import com.quickblox.module.auth.model.QBSession;
import com.quickblox.module.content.model.QBFile;
import com.quickblox.module.custom.model.QBCustomObject;
import com.quickblox.module.custom.model.QBCustomObjectDeleted;
import com.quickblox.module.users.model.QBUser;



public class QBTest {
	
	private static final String className = "Cars";
	String accountKey="LedQjAaawaJkNm3A6Wfk";
	String application_id="3477";
	String auth_key="ChRnwEJ3WzxH9O4";
	String auth_secret="AS546kpUQ2tfbvv";
	String user_id="qb-temp";
	String password="someSecret";

	QB q= new QB();
	
	@Before
	public void setup() throws Exception
	{
		q.connect(accountKey);
		assertThat("api.quickblox.com", is(q.getAPIEndPoint()));
		
		QBSession s= q.session(application_id,auth_key,auth_secret);
		assertThat(s, is(notNullValue()));

		
		QBUser u = q.login("qb-temp", null, "someSecret");
		assertThat(u, is(notNullValue()));
	}

	@Test
	public void testUploadDownload() throws Exception {
		
		Blob b= new Blob("image/bmp", "Bear.bmp","Bear.bmp");
		
		QBFile f= q.uploadContent(b);
		
		assertThat(f,is(notNullValue()));
		assertThat(q.downloadContentByUID(f.getUid(),"test.bmp"),is(notNullValue()));
		
		// now check that two files are not same
	}
	

	@Test
	public void testNonExistentUploadDownload() throws Exception {
		Blob b= new Blob("image/bmp", "non-exist.bmp","non-exist.bmp");
		QBFile f= q.uploadContent(b);
		assertThat(f,is(nullValue()));
	}
	@Test
	public void testCustomObjectFetches() throws Exception {
		assertThat(q.countObjects("WhisperSession"),is(equalTo(0)));
		assertThat(q.countObjects(className),is(greaterThan(250)));
		
		List<QBCustomObject> allObjects = q.getAllObjects(className);
		assertThat(allObjects.size(),is(equalTo(100)));
		
		{
			QBCustomObjectRequestBuilder builder = new QBCustomObjectRequestBuilder();
			builder.eq("make", "Ford");
			allObjects = q.getObjects(className,builder);
			assertThat(allObjects.size(),is(equalTo(100)));
		}
		
		{
			QBCustomObjectRequestBuilder builder = new QBCustomObjectRequestBuilder();
			builder.eq("model", "T");
			allObjects = q.getObjects(className,builder);
			assertThat(allObjects.size(),is(greaterThan(6)));
		}
	}
	@Test
	public void testCustomObjectCRUD() throws Exception {
		
		//create
		String cusId="";
		{
		QBCustomObject qb=new QBCustomObject(className);
		qb.put("make", "Suzuki");
		qb.put("model", "Zen");
		qb.put("value", 178);
		assertThat(q.createObject(qb),is(true));
		cusId=qb.getCustomObjectId();
		}
		//retrieve
		{
			QBCustomObject qb=q.getObject(className, cusId);
			HashMap<String, Object> f = qb.getFields();
			assertThat(f.get("make").toString(),is(equalTo("Suzuki")));
			assertThat(f.get("model").toString(),is(equalTo("Zen")));
			assertThat(f.get("value").toString(),is(equalTo("178")));
		}
		//update
		{
			QBCustomObject qb=new QBCustomObject(className,cusId);
			qb.put("value", 781);
			q.updateObject(qb);
			HashMap<String, Object> f = qb.getFields();
			assertThat(f.get("make").toString(),is(equalTo("Suzuki")));
			assertThat(f.get("model").toString(),is(equalTo("Zen")));
			assertThat(f.get("value").toString(),is(equalTo("781")));
		}

		//increment and decrement
		{
			QBCustomObject qb=new QBCustomObject(className,cusId);
			assertThat(q.incrementFieldBy(qb,"value",3),is(true));
			HashMap<String, Object> f = qb.getFields();
			assertThat(f.get("value").toString(),is(equalTo("784")));
			
			assertThat(q.incrementFieldBy(qb,"value",-2),is(true));
			f = qb.getFields();
			assertThat(f.get("value").toString(),is(equalTo("782")));
		}

		//delete
		{
			QBCustomObject qb=new QBCustomObject(className,cusId);
			assertThat(q.deleteObject(qb),is(true));
		}
		//delete again
		{
			QBCustomObject qb=new QBCustomObject(className,cusId);
			assertThat(q.deleteObject(qb),is(false));
		}
		
		// test inc update
		
		
	}
}

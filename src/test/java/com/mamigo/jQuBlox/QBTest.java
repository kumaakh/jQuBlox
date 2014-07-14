package com.mamigo.jQuBlox;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;



public class QBTest {
	
	String accountKey="LedQjAaawaJkNm3A6Wfk";
	String application_id="3477";
	String auth_key="ChRnwEJ3WzxH9O4";
	String auth_secret="AS546kpUQ2tfbvv";
	String user_id="qb-temp";
	String password="someSecret";


	@Test
	public void testUploadDownload() throws Exception {
		QB q= new QB();
	//	q.connect(accountKey);
		assertThat("https://api.quickblox.com", is(q.getAPIEndPoint()));
		
		q.session(application_id,auth_key,auth_secret);

		Blob b= new Blob("image/bmp", "Bear.bmp","Bear.bmp");
		q.login("qb-temp", null, "someSecret");
		assertThat(q.uploadContent(b),is(true));
		assertThat(q.downloadContent(b.getUID(),"test.bmp"),is(notNullValue()));
		
		// now check that two files are not same
	}
	

	@Test
	public void testNonExistentUploadDownload() throws Exception {
		QB q= new QB();
//		q.connect(accountKey);
		assertThat("https://api.quickblox.com", is(q.getAPIEndPoint()));
		
		q.session(application_id,auth_key,auth_secret);

		Blob b= new Blob("image/bmp", "non-exist.bmp","non-exist.bmp");
		q.login("qb-temp", null, "someSecret");
		assertThat(q.uploadContent(b),is(false));
		
		try{
			q.downloadContent(b.getUID(),"test.bmp");
			assertThat("should throw exception for non-existent", false, is(true));
		}
		catch(IOException th)
		{
			
			assertThat("should throw correct exception", th.getMessage().contains("Not Found"), is(true));
		}
	}
}

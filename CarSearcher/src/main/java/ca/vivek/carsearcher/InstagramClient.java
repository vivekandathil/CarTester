package ca.vivek.carsearcher;

import java.io.IOException;
import java.net.URI;

import me.postaddict.instagram.scraper.AnonymousInsta;
import me.postaddict.instagram.scraper.Instagram;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgentInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgents;
//import static org.assertj.core.api.Assertions.*;
import me.postaddict.instagram.scraper.model.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;



public class InstagramClient 
{
	private static AnonymousInsta client;
	
	public InstagramClient() throws Exception
	{
		
	}
	
	public List<Media> setUp(String hashtag) throws Exception {
        //HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        //loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                //.addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new UserAgentInterceptor(UserAgents.OSX_CHROME))
                .addInterceptor(new ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        client = new Instagram(httpClient);
        client.basePage();
        
        Instagram instagram = new Instagram(httpClient);
        Tag tag = client.getMediasByTag(hashtag, 2);
        List<Media> list = tag.getMediaRating().getMedia().getNodes();

        return list;
    }

}

package yoga1290.printk;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

public class Midan_AttendanceCompetition 
{
	private static String CLIENT_ID="KMZM03DCNBFOS1UDA3LVR5OBGLQQ5IVPZTOJG4XVSF2AZ43E", CLIENT_SECRET="***", redirectURI="http://yoga1290.appspot.com";
	private static void newUser(String access_token) throws Exception
	{
		JSONObject userInfo=Foursquare.getUserInfo(access_token).getJSONObject("response").getJSONObject("user");
		
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Entity member=new Entity(KeyFactory.createKey("midan_4sqr", userInfo.getString("id")));
		member.setProperty("access_token", access_token);
		member.setProperty("name", userInfo.getString("firstName")+" "+userInfo.getString("lastName"));
		datastore.put(member);
	}
	
	private static String getFoursquareMembersAccess() throws Exception
	{
//		JSONObject res=new JSONObject();
		Query q = new Query("midan_4sqr");
		PreparedQuery pq = DatastoreServiceFactory.getDatastoreService().prepare(q);
		int cnt=pq.countEntities(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(10000));
		List<Entity> res= pq.asList(com.google.appengine.api.datastore.FetchOptions.Builder.withLimit(1000));
		String txt="[ \n";
		for(int i=0;i<cnt;i++)
		{
			Entity cur=res.get(i);
			if(!cur.getKey().getName().equals("cache"))
//				txt+=""+new JSONObject(res.get(i).getProperties()).toString();
				txt+="'"+((String)cur.getProperty("access_token"))+"',\n";
		}
		//TODO insert the access tokens in the HTML page
		txt=txt.substring(0, txt.length()-2);
		txt+="]";
		return txt;
	}
	
	public static void doPost(HttpServletRequest req, HttpServletResponse resp)
	{
		try{
			if(!req.getRequestURI().equals("/midan/*********")) return;
			
			Entity cache=new Entity("midan_4sqr","cache");
			String res="";
			InputStream in=req.getInputStream();
			int i;
			byte buff[]=new byte[100];
			while((i=in.read(buff))>0)
				res+=new String(buff,0,i);
			
			cache.setProperty("cache",new Text(res));
			
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			datastore.put(cache);
			
			resp.getWriter().println(":)<br>"+res);
		}catch(Exception e){}
	}
	
	
	public static void doGet(HttpServletRequest req, HttpServletResponse resp)
	{
		try{
			resp.setContentType("text/txt ; charset=utf-8");
			if(req.getRequestURI().equals("/midan/4sqr/"))
			{
				resp.getWriter().println(
						readFromURL("http://yoga1290.appspot.com/midan/4sqr.html").replace("/*cache*/", ((Text)( DatastoreServiceFactory.getDatastoreService().get(KeyFactory.createKey("midan_4sqr", "cache")).getProperty("cache") )).getValue() )
						);
			}
		
			if(req.getRequestURI().equals("/oauth/midan/facebook/callback/"))
			{
				String code=req.getParameter("code");
				// https://www.facebook.com/dialog/oauth?client_id=515920055103374&redirect_uri=http://yoga1290.appspot.com/oauth/midan/facebook/callback/&scope=user_about_me,email,user_education_history&state=4sqr
				if(req.getParameter("state").equals("4sqr")) //1st callback;callback for Auth code
				{
					String access_token=facebook.getAccessToken("515920055103374","***","http://yoga1290.appspot.com/oauth/midan/facebook/callback/",code);
					String id=(String)new JSONObject(facebook.getUser(access_token)).get("id");
					if(id.equals("870205250"))
						resp.getWriter().println(
								readFromURL("http://yoga1290.appspot.com/midan/update4sqr.html").replace("access_token=null", "access_token="+getFoursquareMembersAccess())
								);
					else
						resp.getWriter().println("<script>alert('Not authorized');top.location.href='mailto:yoga1290@gmail.com';</script>");
				}
			}
			
			else if(req.getRequestURI().equals("/oauth/midan/foursquare/callback/"))
			{
				/*
				 * https://foursquare.com/oauth2/authenticate?client_id=KMZM03DCNBFOS1UDA3LVR5OBGLQQ5IVPZTOJG4XVSF2AZ43E&response_type=code&redirect_uri=http://yoga1290.appspot.com/oauth/midan/foursquare/callback/
				 */
				String code=req.getParameter("code");
				String access_token=Foursquare.getAccessToken("KMZM03DCNBFOS1UDA3LVR5OBGLQQ5IVPZTOJG4XVSF2AZ43E", "***", "http://yoga1290.appspot.com/oauth/midan/foursquare/callback/", code);
				newUser(access_token);
				resp.getWriter().println("<script>alert(\"Thanks!É You're on the timepiece graph next update!\");top.location.href='http://yoga1290.appspot.com/midan/4sqr/';</script>");
			}
				
		}catch(Exception e){e.printStackTrace();
			try{resp.getWriter().println(e.getMessage());}catch(Exception e2){}
		}
	}
	
	public static String readFromURL(String rURL)
	{
		String res="";
		try
		{	
			URL url = new URL(rURL);
	
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        InputStream in=connection.getInputStream();
	        byte buff[]=new byte[in.available()];
            int ch;
            while((ch=in.read(buff))!=-1)
            		res+=new String(buff,0,ch);
		}catch(Exception e)
		{
			return e.getMessage();
		}
		return res;		
	}
}

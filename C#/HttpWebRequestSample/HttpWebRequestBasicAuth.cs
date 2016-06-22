using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Net;
using System.IO;
using System.Web;
using System.Configuration;

class HttpWebRequestBasicAuth
{
    static string HttpGet(string url)
    {
        string result = null;
        try
        {
            HttpWebRequest req = WebRequest.Create(url) as HttpWebRequest;
            req.Credentials = GetCredential();
            req.PreAuthenticate = true;

            using (HttpWebResponse resp = req.GetResponse() as HttpWebResponse)
            {
                StreamReader reader = new StreamReader(resp.GetResponseStream());
                result = reader.ReadToEnd();
            }
        }
        catch (Exception ex)
        {
            throw ex;
        }
        return result;
    }

    private static CredentialCache GetCredential()
    {
        string url = @"http://github.com/api/v3/users";
        //ServicePointManager.SecurityProtocol = SecurityProtocolType.Ssl3;
        CredentialCache credentialCache = new CredentialCache();
        //credentialCache.Add(new System.Uri(url), "Basic", new NetworkCredential(ConfigurationManager.AppSettings["gitHubUser"], ConfigurationManager.AppSettings["gitHubUserPassword"]));
        credentialCache.Add(new System.Uri(url), "Basic", new NetworkCredential("huj", "Savit5ch"));
        return credentialCache;
    }

    static string HttpPost(string url,
    string[] paramName, string[] paramVal)
    {
        string result = null;
        try
        {
            HttpWebRequest req = WebRequest.Create(new Uri(url))
                                 as HttpWebRequest;
            req.Method = "POST";
            req.ContentType = "application/x-www-form-urlencoded";

            // Build a string with all the params, properly encoded.
            // We assume that the arrays paramName and paramVal are
            // of equal length:
            StringBuilder paramz = new StringBuilder();
            for (int i = 0; i < paramName.Length; i++)
            {
                paramz.Append(paramName[i]);
                paramz.Append("=");
                paramz.Append(HttpUtility.UrlEncode(paramVal[i]));
                paramz.Append("&");
            }

            // Encode the parameters as form data:
            byte[] formData =
                UTF8Encoding.UTF8.GetBytes(paramz.ToString());
            req.ContentLength = formData.Length;

            // Send the request:
            using (Stream post = req.GetRequestStream())
            {
                post.Write(formData, 0, formData.Length);
            }

            // Pick up the response:        
            using (HttpWebResponse resp = req.GetResponse()
                                          as HttpWebResponse)
            {
                StreamReader reader =
                    new StreamReader(resp.GetResponseStream());
                result = reader.ReadToEnd();
            }

        }
        catch (Exception ex)
        {
            throw ex;
        }

        return result;
    }

    static void Main(string[] args)
    {
        try
        {
            string path = @"http://github.com/api/v3/users";
            string res = HttpWebRequestBasicAuth.HttpGet(path);
            Console.Write(res);
            System.Console.ReadKey(true);              
        }
        catch (Exception ex)
        {
            Console.WriteLine("Exception caught: {0}", ex);
            System.Console.ReadKey(true);              
        }
        
    }
}


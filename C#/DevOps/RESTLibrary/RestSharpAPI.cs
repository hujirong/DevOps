using System;
using System.IO;
using RestSharp;
using RestSharp.Authenticators;

namespace DevOps
{

    /// <summary>
    /// https://github.com/restsharp/RestSharp/wiki/Recommended-Usage
    /// </summary>
    public class RestSharpAPI
    {        
        string baseUrl;

        /// <summary>
        /// Initializes a new instance of the <see cref="RestSharpAPI"/> class.
        /// </summary>
        /// <param name="baseUrl">The base URL.</param>
        public RestSharpAPI(string baseUrl)
        {
            this.baseUrl = baseUrl;
        }


        /// <summary>
        /// Gets the client w/o authentication
        /// </summary>
        /// <returns></returns>
        public RestClient getClient()
        {
            var client = new RestClient();
            client.BaseUrl = new Uri(baseUrl);            
            return client;
        }

        /// <summary>
        /// Get the client using the Basic Authentication.
        /// </summary>
        /// <param name="userId">The user identifier.</param>
        /// <param name="password">The password.</param>
        /// <returns></returns>
        public RestClient getClient(string userId, string password)
        {
            var client = new RestClient();
            client.BaseUrl = new Uri(baseUrl);
            client.Authenticator = new HttpBasicAuthenticator(userId, password);
            return client;
        }

        /// <summary>
        /// Gets the client using OAuth2 token
        /// </summary>
        /// <param name="user">The userid</param>
        /// <param name="token">The token.</param>
        /// <returns></returns>
        public RestClient getClient2(string user, string token)
        {
            RestClient client = new RestClient();
            client.BaseUrl = new Uri(baseUrl);
            client.Authenticator = new HttpBasicAuthenticator(user, token);
            //client.Authenticator = new OAuth2UriQueryParameterAuthenticator(token);  //works
            //client.Authenticator = new OAuth2AuthorizationRequestHeaderAuthenticator(token);  //doesn't work        

            return client;
        }

        public T Execute<T>(RestClient client, RestRequest request) where T : new()
        {
            var response = client.Execute<T>(request);
            if (response.ErrorException != null)
            {
                const string message = "Error retrieving response.  Check inner details for more info.";
                var restSharpAPIException = new ApplicationException(message, response.ErrorException);
                throw restSharpAPIException;
            }
            //Console.WriteLine("Response received: {0}", response.Data);        
            return response.Data;
        }

        public IRestResponse Execute(RestClient client, RestRequest request)
        {
            IRestResponse response = client.Execute(request);
            if (response.ErrorException != null)
            {
                const string message = "Error retrieving response.  Check inner details for more info.";
                var restSharpAPIException = new ApplicationException(message, response.ErrorException);
                throw restSharpAPIException;
            }
            //Console.WriteLine("Response received: {0}", response.ResponseStatus);
            return response;
        }

        public static byte[] file_get_byte_contents(string fileName)
        {
            byte[] sContents;
            if (fileName.ToLower().IndexOf("http:") > -1)
            {
                // URL 
                System.Net.WebClient wc = new System.Net.WebClient();
                sContents = wc.DownloadData(fileName);
            }
            else
            {
                // Get file size
                FileInfo fi = new FileInfo(fileName);

                // Disk
                FileStream fs = new FileStream(fileName, FileMode.Open, FileAccess.Read);
                BinaryReader br = new BinaryReader(fs);
                sContents = br.ReadBytes((int)fi.Length);
                br.Close();
                fs.Close();
            }

            return sContents;
        }

        static void Main(string[] args)
        {
            try
            {
                string baseUrl = "http://github.com/api/v3";
                RestSharpAPI restSharpAPI = new RestSharpAPI(baseUrl);

                Console.ReadKey(true);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Exception caught: {0}", ex);
                Console.ReadKey(true);
            }
        }
    }

}
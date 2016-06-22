using System;
using RestSharp;
using RestSharp.Authenticators;

namespace DevOps
{

    public class RestSharpBasicAuth
    {
        static void Main(string[] args)
        {
            try
            {
                var client = new RestClient();
                client.BaseUrl = new Uri("http://github.com/api/v3");
                client.Authenticator = new HttpBasicAuthenticator("huj", "password");

                var request = new RestRequest();
                request.Resource = "/users/huj";

                IRestResponse response = client.Execute(request);
                Console.WriteLine(response.StatusCode);
                Console.WriteLine(response.Content);

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
using System;
using System.IO;
using RestSharp;
using System.Collections.Generic;
using System.Text.RegularExpressions;

namespace DevOps
{
    /// <summary>
    /// https://github.com/restsharp/RestSharp/wiki/Recommended-Usage
    /// </summary>
    class UCDAPI
    {
        
        RestClient client;
        RestSharpAPI restSharpAPI;
        //public static string baseUrl = "http://localhost:8060/nexus/service/local";
        public static string baseUrl = "https://localhost:8443/cli/";
        static string user = "admin";
        static string password = "admin";

        List<string> urls = new List<string>(); // foler URL to be deleted
           
        public UCDAPI(RestSharpAPI restSharpAPI, RestClient client)
        {
            this.restSharpAPI = restSharpAPI;
            this.client = client;
        }

        public UCDComponent GetUCDComponent(string name)
        {
            RestRequest request = new RestRequest();
            request.Resource = "component/info?component=" + name;
            // https://localhost:8443/cli/component/info?component=MyCRM
            request.RootElement = "UCDComponent";

            return restSharpAPI.Execute<UCDComponent>(this.client, request);
        }

        public void createUCDComponent(string componentName)
        {
            RestRequest request = new RestRequest("/component/create", Method.POST);
            // Below two lines are optional
            //request.RequestFormat = RestSharp.DataFormat.Json;
            //request.AddHeader("Content-Type", "x-www-form-urlencoded");
            
            request.AddHeader("Content-type", "application/json");
            request.AddJsonBody(
                new
                {
                    name = componentName,
                    description = "",
                    importAutomatically = "false",
                    useVfs = "false",
                    sourceConfigPlugin = "",
                    defaultVersionType = "FULL"
                });

            IRestResponse response = this.restSharpAPI.Execute(this.client, request);
            Console.WriteLine("Response received: {0} {1}", response.ResponseStatus, response.ResponseUri);

        }

        // not sure if this will work
        public UCDComponent CreateUCDComponent()
        {
            RestRequest request = new RestRequest();
            request.Resource = "/component/create";            
            request.RootElement = "UCDComponent";
            request.AddHeader("Content-type", "application/json");
            request.AddJsonBody(
                new
                {
                    name = "MyGIS",
                    description = "New component for command example",
                    importAutomatically = "false",
                    useVfs = "false",
                    sourceConfigPlugin = "",
                    defaultVersionType = "FULL"                     
            }); 

            return restSharpAPI.Execute<UCDComponent>(this.client, request);
        }

        public static void IgnoreBadCertificates()
        {
            System.Net.ServicePointManager.ServerCertificateValidationCallback = new System.Net.Security.RemoteCertificateValidationCallback(AcceptAllCertifications);
        }
        /// <summary>
        /// In Short: the Method solves the Problem of broken Certificates.
        /// Sometime when requesting Data and the sending Webserverconnection
        /// is based on a SSL Connection, an Error is caused by Servers whoes
        /// Certificate(s) have Errors. Like when the Cert is out of date
        /// and much more... So at this point when calling the method,
        /// this behaviour is prevented
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="certification"></param>
        /// <param name="chain"></param>
        /// <param name="sslPolicyErrors"></param>
        /// <returns>true</returns>
        private static bool AcceptAllCertifications(object sender, System.Security.Cryptography.X509Certificates.X509Certificate certification, System.Security.Cryptography.X509Certificates.X509Chain chain, System.Net.Security.SslPolicyErrors sslPolicyErrors)
        {
            return true;
        }

        static void Main(string[] args)
        {
            try
            {
                IgnoreBadCertificates();
                RestSharpAPI restSharpAPI = new RestSharpAPI(baseUrl);
                //RestClient client = restSharpAPI.getClient();  //works
                RestClient client = restSharpAPI.getClient(user, password); // works

                if (restSharpAPI == null || client == null)
                {
                    Console.WriteLine("restSharpAPI or client is null");
                    Environment.Exit(1);
                }

                UCDAPI UCDAPI = new UCDAPI(restSharpAPI, client);
                Console.WriteLine("restSharpAPI or client is good to go.");
                UCDComponent component = UCDAPI.GetUCDComponent("IIB Broker");
                Console.WriteLine("UCD component MyCRM:" + component.ToString());

                UCDAPI.createUCDComponent("MyGIS1");

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
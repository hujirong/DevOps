using RestSharp.Deserializers;

namespace DevOps
{
    /// <summary>
    /// Search result from http://localhost:8060/nexus/service/local/search/m2/ngfreeform?p=environment&t=equal&v=DEV
    /// </summary>
    [DeserializeAs(Name = "content")]
    class NexusContent
    {
        public string text { get; set; }
        public string lastModified { get; set; }
        public string resourceURI { get; set; }
        public string relativePath { get; set; }
        public string leaf { get; set; }

        public override string ToString()
        {
            string s = " ";
            return s;
        }
    }
}

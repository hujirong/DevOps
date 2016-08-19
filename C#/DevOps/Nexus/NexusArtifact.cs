using RestSharp.Deserializers;

namespace DevOps
{
    /// <summary>
    /// Search result from http://localhost:8060/nexus/service/local/search/m2/ngfreeform?p=environment&t=equal&v=DEV
    /// </summary>
    [DeserializeAs(Name = "artifact")]
    class NexusArtifact
    {
        public string groupId { get; set; }
        public string artifactId { get; set; }
        public string version { get; set; }
        public string latestRelease { get; set; }

        public override string ToString()
        {
            string s = "groupId=" + groupId + " artifactId=" + artifactId + " version=" + version + " latestRelease=" + latestRelease;
            return s;
        }
    }
}

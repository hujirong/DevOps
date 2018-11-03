using RestSharp.Deserializers;

namespace DevOps
{
    /// <summary>
    /// This is a copy of NextArtifacts.cs, just to make the build happy
    /// </summary>
    [DeserializeAs(Name = "artifact")]
    class NexusContent
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

namespace DevOps
{

    class GitHubTeam
    {
        public string name { get; set; }
        public string id { get; set; }

        public override string ToString()
        {
            string s = "name=" + name + " id=" + id;
            return s;
        }
    }

}
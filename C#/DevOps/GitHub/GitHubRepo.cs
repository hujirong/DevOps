namespace DevOps
{

    class GitHubRepo
    {
        public string name { get; set; }

        public override string ToString()
        {
            string s = "name=" + name;
            return s;
        }
    }

}
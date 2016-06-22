namespace DevOps
{

    class GitHubUser
    {
        public string login { get; set; }
        public string id { get; set; }
        public string ldap_dn { get; set; }

        public override string ToString()
        {
            string s = "login=" + login + " id=" + id + " ldap_dn=" + ldap_dn;
            return s;
        }
    }

}
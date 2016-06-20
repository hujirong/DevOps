using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Security.Cryptography;
using System.Security;
using System;
using System.Configuration;



class RSACipher
{
    public RSACipher() { }

    /// <summary>
    /// This method is used to encrypt the particular section in the application config
    /// </summary>
    /// <param name="section">Section to encrypt</param>
    private static void EncryptAppSettings(string section)
    {
        Configuration objConfig = ConfigurationManager.OpenExeConfiguration(GetAppPath() + "githubSuspsendInactiveADAccounts.exe");
        AppSettingsSection objAppsettings = (AppSettingsSection)objConfig.GetSection(section);
        if (!objAppsettings.SectionInformation.IsProtected)
        {
            objAppsettings.SectionInformation.ProtectSection("RsaProtectedConfigurationProvider");
            objAppsettings.SectionInformation.ForceSave = true;
            objConfig.Save(ConfigurationSaveMode.Modified);
        }
    }

    /// <summary>
    /// This method is used to fetch the location of location of executable
    /// </summary>
    /// <returns>location of executable</returns>
    private static string GetAppPath()
    {
        System.Reflection.Module[] modules = System.Reflection.Assembly.GetExecutingAssembly().GetModules();
        string location = System.IO.Path.GetDirectoryName(modules[0].FullyQualifiedName);
        if ((location != "") && (location[location.Length - 1] != '\\'))
            location += '\\';
        return location;
    }

    /// <summary>
    /// This method is used to decrypt the particular section of the application config
    /// </summary>
    /// <param name="section">Section to encrypt</param>
    private static void DecryptAppSettings(string section)
    {
        Configuration objConfig = ConfigurationManager.OpenExeConfiguration(GetAppPath() + "githubSuspsendInactiveADAccounts.exe");
        AppSettingsSection objAppsettings = (AppSettingsSection)objConfig.GetSection(section);
        if (objAppsettings.SectionInformation.IsProtected)
        {
            objAppsettings.SectionInformation.UnprotectSection();
            objAppsettings.SectionInformation.ForceSave = true;
            objConfig.Save(ConfigurationSaveMode.Modified);
        }
    }

    /// <summary>
    /// This method is used to update the key value
    /// </summary>
    /// <param name="newValue">New value of the key</param>
    private static void UpdateKey(string newValue)
    {
        ExeConfigurationFileMap configFile = new ExeConfigurationFileMap();
        configFile.ExeConfigFilename = ConfigurationManager.AppSettings["configPath"];
        Configuration config = ConfigurationManager.OpenMappedExeConfiguration(configFile, ConfigurationUserLevel.None);
        config.AppSettings.Settings["password"].Value = newValue;
        config.Save();
    }

    static void Main(string[] args)
    {
        string gitHubAPITokenWithoutEncryption = ConfigurationManager.AppSettings["gitHubAPIToken"];
        Console.WriteLine("gitHubAPITokenWithoutEncryption={0}", gitHubAPITokenWithoutEncryption);

        EncryptAppSettings("appSettings");
        string gitHubAPITokenWithEncrytionApplied = ConfigurationManager.AppSettings["gitHubAPIToken"];
        Console.WriteLine("gitHubAPITokenWithEncrytionApplied={0}", gitHubAPITokenWithEncrytionApplied);

        //System.Console.ReadKey(true);
    }

}


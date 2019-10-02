#! perl
#
# This script will query a ClearQuest database for all activities whose 
# UCM project is the specified project and whose state is in 
# the given statelist and for each of those, will extract 
# it's change set.  The result will be a report sent to 
# standard out or to MS Excel depending on whether you specify
# the -excel option.
#
use Win32::OLE;
use Getopt::Long;
use Tk;

#-------------------------------------------------------------------------------
# Some useful constants for the benefit of the CQ API
#-------------------------------------------------------------------------------
$AD_PRIVATE_SESSION = 2;	# Only one client can access this session’s data
$AD_BOOL_OP_AND = 1;	# Boolean AND operator
$AD_BOOL_OP_OR = 2;	# Boolean OR operator

$AD_COMP_OP_EQ = 1;	# Equality operator
$AD_COMP_OP_NEQ = 2;	# Inequality operator

$AD_SUCCESS = 1;	# The next record in the request set was successfully obtained

#-------------------------------------------------------------------------------
# Setting the $DEBUG variable will cause verbose printing
#    of debugging messages
#-------------------------------------------------------------------------------
# $DEBUG = 1;

#-------------------------------------------------------------------------------
# Info for help messages
#-------------------------------------------------------------------------------
my ($NAME, $THISDIR) = ($0 =~ m#^(.*)[/|\\]([^/|\\]+)$#o) ? ($2, $1) : ($0, '.');

$SYNOPSIS = "$NAME [-help] {-cqlogin clearquest_login_id} 
			   {-cqpasswd clearquest_password}
			   {-cqdb clearquest_database_id}
			   {-project project_name}
			   {-statelist list_of_states}
			   [-excel]
			   [{-cli]
";

$ARGUMENTS = "
  help		Displays this message and exits.
  cqlogin	A valid ClearQuest login ID
  cqpasswd	The password associated with the given ClearQuest login ID
  cqdb		The logical 5-character name of the ClearQuest database
  		to log into.
  project	The project on which to report
  statelist	A list of states to refine the query (comma-separated, no
  		spaces)
  excel		Export report to Excel spreadsheet instead of printing it
  		to the screen
  cli		Don't put up the GUI asking for info.  Take it from
  		the command-line only
";

$DESCRIPTION = "
$NAME will query a ClearQuest database for all activities whose 
	UCM project is the specified project and whose state is in 
	the given statelist and for each of those, will extract 
	it's change set.  The result will be a report sent to 
	standard out or to MS Excel depending on whether you specify
	the -excel option.
";

#-------------------------------------------------------------------------------
# Parse out the command line
#-------------------------------------------------------------------------------
my ($return_status) = GetOptions("cqlogin=s", "cqpasswd=s", "cqdb=s", 
				"project=s", "statelist=s", "excel", 
				"cli", "help");

#-------------------------------------------------------------------------------
# If the "-help" option is set, then print out a usage message
#    and exit
#-------------------------------------------------------------------------------
usage(1) if ($opt_help);

#-----------------------------------------------------
# Create ClearQuest session object
#-----------------------------------------------------
print "Creating ClearQuest Session\n" if $DEBUG;
my ($CQsession) = Win32::OLE->new ("CLEARQUEST.SESSION") or
   die "Can't create ClearQuest session object via call to Win32::OLE->new(): $!";

#-----------------------------------------------------
#  Get the list of CQ databases to which we can try to log into
#-----------------------------------------------------
$AccDatabases = $CQsession->GetAccessibleDatabases("MASTR", "", "");

#-----------------------------------------------------
#  Now put those database names into a Perl list
#-----------------------------------------------------
$DBCount=@$AccDatabases;
@dblist = ();
for ($i=0; $i<$DBCount; $i++) {
	$aDatabase = $AccDatabases->[$i];
  	$DBName = $aDatabase->GetDatabaseName();
  	@dblist = ($DBName, @dblist);
  	print "DB Name is $DBName\n" if $DEBUG;
}

#-----------------------------------------------------
#  Use a GUI interface to make things easier to run
#-----------------------------------------------------
if (! $opt_cli) {
	print "Painting GUI\n" if $DEBUG;

	$top = MainWindow->new();
	$top->configure("title"=>"ChangeSet Report");
	$top->raise();

	$LoginFrame = $top->Frame(-width=>'50', -relief=>'ridge', 
				-borderwidth=>'3')-> pack(-expand=>'1', 
				-fill=>'both');
	$LoginFrame->Label(-text=>"ClearQuest Login ID")->pack( -expand=>'1');
	$LoginBox = $LoginFrame->Entry(-textvariable=>\$opt_cqlogin, 
			-width=>'25')->pack( -expand=>'1');
	$LoginFrame->Label(-text=>"ClearQuest Password")->pack( -expand=>'1');
	$PasswordBox = $LoginFrame->Entry(-textvariable=>\$opt_cqpasswd, 
			-width=>'25', -show=>"*")->pack( -expand=>'1',);

	$LoginFrame->Label(-text=>"ClearQuest Database")->pack( -expand=>'1');
	$DbBox = $LoginFrame->Optionmenu( -options => [ @dblist ],
			-textvariable=>\$opt_cqdb, 
			-width=>'7')->pack( -expand=>'1');

	$QueryFrame = $top->Frame(-width=>'40', -label=>"UCM Query Info",
					-relief=>'ridge', -borderwidth=>'3')->
					pack(-expand=>'1', -fill=>'both');
	$QueryFrame->Label(-text=>"UCM Project")->pack( -expand=>'1');
	$ProjectBox = $QueryFrame->Entry(-textvariable=>\$opt_project, 
			-width=>'25')->pack(-expand=>'1');;
	$QueryFrame->Label(-text=>"ClearQuest State List")->pack( -expand=>'1');
	$StateBox = $QueryFrame->Entry(-textvariable=>\$opt_statelist, 
			-width=>'35')->pack(-expand=>'1');

	$EndFrame = $top->Frame(-width=>'40')->pack(-pady=>'10');
	$ExcelButton = $EndFrame->Checkbutton(-text=> "Send Report to Excel", -variable=>\$opt_excel)->pack(-side=>'top');
	$OKButton = $EndFrame->Button(-text => "OK", -width=>'10',
				-command => sub { $top->destroy() })->pack(
				-expand=>1,-padx=>'10',-pady => '5', -side=>'left');
				;
	$CancelButton = $EndFrame->Button(-text => "Cancel", -width=>'10',
				-command => sub { exit })->pack(-expand=>1,
				-padx=>'10',-pady => '5', -side=>'right');
	MainLoop();
}


#-----------------------------------------------------
# If we are using the command line, and the CQ Db wasn't
#   specified, then issue a usage message and exit
#-----------------------------------------------------
if ($opt_cli) {
	usage(2) if ( ! "$opt_cqdb" );
}

#-----------------------------------------------------
# General testing for required values
#-----------------------------------------------------
usage(2) if ( @ARGV or ! $return_status );
usage(2) if ( ! "$opt_cqlogin" );
usage(2) if ( ! "$opt_cqpasswd" );
usage(2) if ( ! "$opt_project" );
usage(2) if ( ! "$opt_statelist" );

$loginid = $opt_cqlogin;
$password = $opt_cqpasswd;
$database = $opt_cqdb;
$ucm_project = $opt_project;

#-----------------------------------------------------
# Remove whitespace from the statelist
#-----------------------------------------------------
($statelist = $opt_statelist) =~ s/ //g;

print "DEBUG: CQ Login is $loginid\n" if $DEBUG;
print "DEBUG: CQ Password is $password\n" if $DEBUG;
print "DEBUG: CQ Database is $database\n" if $DEBUG;
print "DEBUG: CQ Project is $ucm_project\n" if $DEBUG;
print "DEBUG: CQ State List is $statelist\n" if $DEBUG;

#-----------------------------------------------------
# Parse out the list of states from the statelist
#-----------------------------------------------------

@states = split /,/,$statelist;

#-----------------------------------------------------
# Log into ClearQuest
#-----------------------------------------------------

print "Logging onto ClearQuest Session\n" if $DEBUG;
$CQsession->UserLogon("$loginid", "$password", "$database", $AD_PRIVATE_SESSION, "");

#----------------------------------------------------
# Get a ClearQuest Query Def object
#    We will query on All UCM Activities
#----------------------------------------------------
print "Building ClearQuest Query\n" if $DEBUG;
my ($QueryDef) = $CQsession->BuildQuery("All_UCM_Activities");

#----------------------------------------------------
# Specify fields to get as a result of the ClearQuest 
# Query
#
# This will be the column numbers in this order,
# starting at 1, not 0.
#----------------------------------------------------

$QueryDef->BuildField("id");
$QueryDef->BuildField("State");
$QueryDef->BuildField("headline");
$QueryDef->BuildField("Owner");
#----------------------------------------------------
# The ucm_vob_object field in CQ holds a CC internal
#   identifier for the UCM activity associated with the
#   CQ record
#----------------------------------------------------
$QueryDef->BuildField("ucm_vob_object");

#--------------------------------------------------------
# Specify the ClearQuest Query filter tree
#   ucm_project = specified project
#   list of states = specified list of states
#--------------------------------------------------------
my ($FilterNode1) = $QueryDef->BuildFilterOperator($AD_BOOL_OP_AND);
$FilterNode1->BuildFilter("ucm_project", $AD_COMP_OP_EQ, "$ucm_project");
my ($FilterNode2) = $FilterNode1->BuildFilterOperator($AD_BOOL_OP_OR);
foreach $st (@states) {
	$FilterNode2->BuildFilter("State", $AD_COMP_OP_EQ, "$st");
	print "DEBUG: CQ State is $st\n" if $DEBUG;
}

#------------------------------------------------------------
# Create the ClearQuest result set and execute
#------------------------------------------------------------
print "Executing ClearQuest Query\n" if $DEBUG;
my ($ResultSet) = $CQsession->BuildResultSet($QueryDef);
$ResultSet->Execute();

#------------------------------------------------------------
# Where do we send the results...
#    To Excel or to standard out?
#------------------------------------------------------------
if ("$opt_excel" ne "1") {
   print("\nResult set of query for record type ", 
	$ResultSet->LookupPrimaryEntityDefName(),"\n\n");
} else {
   $Excel = Win32::OLE->new("Excel.Application") or
      die "Can't create Excel application object via call to Win32::OLE->new(): $!";

#------------------------------------------------------------
# Make Excel visible, add a workbook and then populate the
#   column headers for this report
#------------------------------------------------------------
   $Excel->{visible} = 1;
   $Excel->Workbooks->Add();
   $Excel->Range("A1")->{Value} = "ID";
   $Excel->Range("B1")->{Value} = "State";
   $Excel->Range("C1")->{Value} = "Owner";
   $Excel->Range("D1")->{Value} = "Headline";
   $Excel->Range("E1")->{Value} = "Change Set";
}

#----------------------------------------------------------------
# loop through the CQ Query results.  Use an initial MoveNext to 
#    get first record
#----------------------------------------------------------------
print "Looping through query results\n" if $DEBUG;
$xlix = 1;
$records = 0;
$status = $ResultSet->MoveNext();
while ( $status == $AD_SUCCESS ) {     
	$records++;
	$xlix++;

	print "Processing query result $records\n" if $DEBUG;

#----------------------------------------------------------------
# Get the field values from the returned query result
#----------------------------------------------------------------
	$id = $ResultSet->GetColumnValue(1);
	$state = $ResultSet->GetColumnValue(2);
	$headline = $ResultSet->GetColumnValue(3);
	$owner = $ResultSet->GetColumnValue(4);
#----------------------------------------------------------------
# Stuff them into Excel or print them to standard out
#----------------------------------------------------------------
	if ("$opt_excel" eq "1") {
           $Excel->Range("A$xlix")->{Value} = $id;
	   $Excel->Range("B$xlix")->{Value} = $state;
	   $Excel->Range("C$xlix")->{Value} = $owner;
	   $Excel->Range("D$xlix")->{Value} = $headline;
	} else {
	   write();
	}
#----------------------------------------------------------------
# Get the CC activity ID from the CQ "ucm_vob_object" field
#----------------------------------------------------------------
	$activity_id = $ResultSet->GetColumnValue(5);
#----------------------------------------------------------------
# If the activity ID from CQ is null, then say that we have no 
#    change set info
#----------------------------------------------------------------
	if (! $activity_id ) {
	   if ("$opt_excel" ne "1") {
      		print "     Change Set Info:\n";
      		print "     ----------------\n";
		print "     No ClearCase Activity info yet\n";
	   }
	} else {
#----------------------------------------------------------------
# If the CC activity ID is not null, then we need to use CAL
#    to extract the changeset info from the CC activity object
#----------------------------------------------------------------
		print "Instantiating CAL\n" if $DEBUG;
		my ($CCApp) = Win32::OLE->new ("ClearCase.Application") or
            		die "Can't create ClearCase application object via call to Win32::OLE->new(): $!";
		print "Processing Activity info\n" if $DEBUG;
#----------------------------------------------------------------
# Get an activity object from CAL
#----------------------------------------------------------------
      		$myactivity = $CCApp->Activity($activity_id);
		if (! $myactivity ) {
			print "Can not resolve activity info in ClearCase\n";
		} else {
      		   $view = $myactivity->NameResolverView;

#----------------------------------------------------------------
# Get the activity's change set, which is a CCVersions collection. 
#   Use the activity's "nameresolver view" for name resolution.
#----------------------------------------------------------------
      		   $ChangeSet = $myactivity->ChangeSet($view, "False");
      		   $CS_Entries = $ChangeSet->Count;

#----------------------------------------------------------------
# Loop through the CCVersions collection, collecting the names of 
#     the versions for printing. 
#----------------------------------------------------------------
		   print "Getting ChangeSet info\n" if $DEBUG;
      		   $CS_Index = 1;
      		   while ($CS_Index <= $CS_Entries) {
		      print "Processing ChangeSet entry $CS_Index\n" if $DEBUG;
	   	      $Version = $ChangeSet->Item($CS_Index);
	   	      $VersionPN = $Version->ExtendedPath;
		      if ("$CS_Index" eq "1") {
		         $cs_list = $VersionPN;
		      } else {
		         $cs_list = $cs_list . "\n" . $VersionPN;
		      }
	   	      $CS_Index++;
      		   }

		   print "Printing ChangeSet\n" if $DEBUG;
		   if ("$opt_excel" eq "1") {
	   	      $Excel->Range("E$xlix")->{Value} = "$cs_list";
		   } else {
      		      print "Change Set Info:\n";
      		      print "----------------\n";
		      print "$cs_list\n";
	   	      print "\n";
		   }
		}
	   }
	   $status = $ResultSet->MoveNext();
}
if ("$opt_excel" ne "1") {
   print("\n$records activities were found\n");
   print "Press any key to exit\n";
   $ans=<STDIN>;;
}
exit(0);

#---------------------------------------------------------
# print out help if needed
#---------------------------------------------------------
sub usage {
   my ($level) = @_;

   $level = 2 unless ( $level =~ /^\d+$/o );

   print "\nUsage: ${SYNOPSIS}\n";
   print "\nArguments:${ARGUMENTS}${DESCRIPTION}\n" unless ( $level > 1 );
   exit($level);
}

format STDOUT_TOP =
ID             State       Owner	Headline                            
-------------  ---------   ----------   -------------------------------------- 
.

format STDOUT =
@<<<<<<<<<<<<  @<<<<<<<<<  @<<<<<<<<<  ^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
$id, $state,	$owner                 $headline
~~                                        ^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                                              $headline
.

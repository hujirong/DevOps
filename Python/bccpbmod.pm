package bccpbmod;

# bccpbmod.pm - Common module defining global variables & shared subroutines
#
# (c) Copyright IBM Corp. 2009
#
# This is the common routines used in all the Build scripts.
#
# You must modify this script before attempting to use parameterized build.
# Sections that require modification and the instructions for modifying them
# are flagged with the following comment lines:
#
use File::Path;
use File::Copy;
use File::Find;
use POSIX qw(strftime);

print "\n***** start bccpbmod.pl *****\n" if $debug;

# ******************************************************************************
# * Customization section:                                                     *
# ******************************************************************************
$PVOB        = "\\Mainframe_PVOB";
$hl2         = "A";
$CLEARPROMPT = "$ENV{'CLEARPROMPT'}";
print "CLEARPROMPT=$CLEARPROMPT\n" if $debug;

$run_env = bccpbmod::find_env();

if ( $run_env eq "PROD" ) {

    # Share control variables with other modules
    use vars qw( $debug, $clearprompt );

    $debug = 1;    # Setting the script verbose debugging output
    $clearprompt =
      1;  # Defines the way files types are entered during add to source control

    # Share global build control variables with other modules
    use vars
      qw( $preview, $single_bcl, $rccVerbosity, $network_name, $cpy_dir, $batch_dir, $cics_dir, $dbrm_dir, $sysout );

    $preview      = 0;           #
    $single_bcl   = 0;           #
    $auth_level   = 2;           #
    $rccVerbosity = "";          # Setting verbosity level
    $rccVerbosity = '-V -V -V'
      if $debug; # If debug on this will display all output at the fullest level
    $network_name = "view";                                   #
    $cpy_dir      = 'COPY\\COBC';                             # Not in use
    $batch_dir    = 'COPY\\COBC';                             # Not in use
    $cics_dir     = 'COPY\\COBC';                             # Not in use
    $dbrm_dir     = 'COPY\\COBC';                             # Not in use
    $sysout       = "SYSOUT=*";
    $sysout       = "RCCEXT=RCCOUT,DISP=(NEW,CATLG,DELETE),
//            SPACE=(32000,(30,30))"
      if $debug;    # Sends the SYSOUT output to ClearCase view

    # Share ClearQuest variables with other modules
    use vars qw( $CQ_enabled, $CQ_user, $CQ_pw, $CQ_dbname, $CQ_dbset );

    $CQ_enabled = 1;                  # Setting the CC/CQ UCM integration
    $CQ_user    = "admin";            # ClearQuest username
    $CQ_pw      = "admin";            # ClearQuest password
    $CQ_dbname  = "USR02";            # ClearQuest user database name
    $CQ_dbset   = "HBC_CQ_PRD_V8";    # ClearQuest dbset name

    # Share environment files variables with other modules
    use vars
      qw( $scriptdir, $envs_file, $members_file, $types_file, $userids_file );

    $scriptdir     = '\\\jxn-ms-cccqp01\CCSTG\triggers';
    $envs_file     = 'envs.txt';                           #
    $members_file  = 'members.txt';                        #
    $types_file    = 'types.txt';                          #
    $userids_file  = 'userids.txt';                        #
    $db2_file      = 'db2.txt';                            #
    $cics_file     = 'cics.txt';                           #
    $software_file = 'software.txt';                       #
    $ora_idms_file = 'oracle_idms.txt';                    #
    $eos_file      = 'eos.txt';                            #

    # Share ClearCase variables with other modules
    use vars qw( $rccbuild, $cleartool, $ccperl );

    $rccbuild  = 'rccbuild.exe';
    $cleartool = 'cleartool.exe';
    $ccperl    = 'ccperl.exe';

    # Local variables
    $vb_user = 'ccadm01';     # Account used to run the chtype command
    $vb_pw   = 'd2$chez1';    # Account password
}

elsif ( $run_env eq "LAB" ) {

    # Share control variables with other modules
    use vars qw( $debug, $clearprompt );

    $debug = 1;               # Setting the script verbose debugging output

    if ( $CLEARPROMPT eq 'BF' ) {    # Can't prompt if BF project is called, or during a deliver
        $clearprompt = 0;
    }
    else {
        $clearprompt = 1;   # For direct menu action "Build on Host", wait for user to verify
    }

    # Share global build control variables with other modules
    use vars
      qw( $preview, $single_bcl, $rccVerbosity, $network_name, $cpy_dir, $batch_dir, $cics_dir, $dbrm_dir, $sysout );

    $preview      = 0;           #
    $single_bcl   = 0;           #
    $auth_level   = 2;           #
    $rccVerbosity = "";          # Setting verbosity level
    $rccVerbosity = '-V -V -V'
      if $debug; # If debug on this will display all output at the fullest level
    $network_name = "view";                                   #
    $cpy_dir      = 'COPY\\COBC';                             # Not in use
    $batch_dir    = 'COPY\\COBC';                             # Not in use
    $cics_dir     = 'COPY\\COBC';                             # Not in use
    $dbrm_dir     = 'COPY\\COBC';                             # Not in use
    $sysout       = "SYSOUT=*";
    $sysout       = "RCCEXT=RCCOUT,DISP=(NEW,CATLG,DELETE),
//            SPACE=(32000,(30,30))"
      if $debug;    # Sends the SYSOUT output to ClearCase view

    # Share ClearQuest variables with other modules
    use vars qw( $CQ_enabled, $CQ_user, $CQ_pw, $CQ_dbname, $CQ_dbset );

    $CQ_enabled = 1;                  # Setting the CC/CQ UCM integration
    $CQ_user    = "admin";            # ClearQuest username
    $CQ_pw      = "admin";            # ClearQuest password
    $CQ_dbname  = "USR02";            # ClearQuest user database name
    $CQ_dbset   = "HBC_CQ_PRD_V8";    # ClearQuest dbset name

    # Share environment files variables with other modules
    use vars
      qw( $scriptdir, $envs_file, $members_file, $types_file, $userids_file );

    $scriptdir     = '\\\jxn-ms-cccqp01\CCSTG\triggers';
    $envs_file     = 'envs.txt';                           #
    $members_file  = 'members.txt';                        #
    $types_file    = 'types.txt';                          #
    $userids_file  = 'userids.txt';                        #
    $db2_file      = 'db2.txt';                            #
    $cics_file     = 'cics.txt';                           #
    $software_file = 'software.txt';                       #
    $ora_idms_file = 'oracle_idms.txt';                    #
    $eos_file      = 'eos.txt';                            #

    # Share ClearCase variables with other modules
    use vars qw( $rccbuild, $cleartool, $ccperl );

    $rccbuild  = 'rccbuild.exe';
    $cleartool = 'cleartool.exe';
    $ccperl    = 'ccperl.exe';

    # Local variables
    $vb_user = 'ccadm01';     # Account used to run the chtype command
    $vb_pw   = 'd2$chez1';    # Account password
}

elsif ( $run_env eq "" ) {
    $cmd =
`clearprompt proceed -type error -default abort -mask proceed,abort -prompt \"Can't define the execution environment.\nAborting...\" -newline`
      if $clearprompt;
    print "Can't define the execution environment.\nAborting...\n";
    exit 1;
}

# ******************************************************************************
# * End of customization section:                                              *
# ******************************************************************************

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  User ID Table  >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# getUserID uses a lookup table to translate the users' PC username to their
# mainframe ID.
#
# This routine can be rewritten to query a database for the mainframe ID.

sub getUserIDTC {
    print "\n*** bccpbmod::getUserID ***\n" if $debug;
    my ($pcid) = @_;
    my $hostid = "";    # User ID Table
    print "\nPCID from getUserID: $pcid\n" if $debug;

    # Check if the scriptdir is defined
    $stream = `cleartool lsstream -fmt \"%[name]Xp\"`;
    if ( $scriptdir eq "" ) {
        $scriptdir = `cleartool describe -fmt \"%[SCRIPTDIR]SNa\" $stream`;
        if ( $scriptdir eq "" ) {
            die "\nThe build script directory not found! Aborting...\n";
        }
        $scriptdir =~ s/^\"//;    # remove quotes around attr value
        $scriptdir =~ s/\"$//;    # returned from cleartool cmd
        print "\nBuild script dir by attribute: $scriptdir\n" if $debug;
    }
    else {
        print "\nBuild script dir by bccpbmod.pm: $scriptdir\n" if $debug;
    }

    $file = $scriptdir . "\\$userids_file";
    my $pcname  = "";
    my $mvsname = "";
    my $mfid    = "";
    open( USERIDS, $file ) or die "Can't open $file file: $!\n";
    while (<USERIDS>) {
        chomp;
        ( $pcname, $mvsname ) = split(/\t/);
        if ( lc $pcname eq lc $pcid ) {
            $mfid = $mvsname;
            print "MFID from userid table: $mfid \n" if $debug;
        }
    }
    return $mfid;
}

sub getUserID {
    print "\n*** bccpbmod::getUserID ***\n" if $debug;
    my ($pcid) = @_;
    my $hostid = "";    # User ID Table
    print "\nPCID from getUserID: $pcid\n" if $debug;

    return uc($pcid);
}

# Sort streams in DEV,TST,PAT,QEM sequence, fix for v8
sub sort_streams {
    my @stream_before = @_;
    @DEV = grep( /DEV/, @stream_before );
    @TST = grep( /TST/, @stream_before );
    @PAT = grep( /PAT/, @stream_before );
    @QEM = grep( /QEM/, @stream_before );
    print "\@DEV=@DEV\n";
    print "\@TST=@TST\n";
    print "\@PAT=@PAT\n";
    print "\@QEM=@QEM\n";
    my @stream_after = ( @PAT, @QEM, @TST, @DEV );
    return @stream_after;
}

# <<<<<<<<<<<<<<< Parase deliver activity and return unique BaseCMActivity <<<<<<<<<<<<<<<
# Input: deliver UCMUntilityActivity: $DLVR_ACTS
# Return: list of unique BaseCMActivity: @CR_UNIQUE
#########################################################################################
sub getUniqueCR {
    my ( $DLVR_ACTS, $PVOB ) = @_;
    print "# DLVR_ACTS=$DLVR_ACTS\n" if $debug;
    my @D_ACTS = split( / /, $DLVR_ACTS );
    my @CRS, @CR_UNIQUE;
    my $cmd;
    foreach my $d_act (@D_ACTS) {
        my $type =
          `cleartool describe -fmt %[crm_record_type]p activity:$d_act`;
        my $headline = `cleartool describe -fmt %[headline]p activity:$d_act`;
        print "# d_act=$d_act;type=$type;headline=$headline\n" if $debug;

        # Exclude rebase activity
        if ( ( $type eq "UCMUtilityActivity" ) && ( $headline =~ /rebase/ ) ) {
            print "# Deliver activity: $d_act is a rebase activity, skip.\n"
              if $debug;
            next;
        }

        # Deliver from DEV and QEM
        if ( $type eq "BaseCMActivity" ) {
            print "# Deliver activity: $d_act is a BaseCMActivity.\n" if $debug;
            $d_act =~ /(.+)\@(.+)/;
            my $CRID = $1;
            unshift( @CRS, "$CRID" );
            next;
        }

        if ( ( $type eq "UCMUtilityActivity" ) && ( $headline =~ /deliver/ ) )
        {    # parse deliver activity - start
            print
"# Deliver activity: $d_act is a deliver activity, retrieve the contributing activities.\n"
              if $debug;
            $cmd = `cleartool describe -fmt %[contrib_acts]Cp activity:$d_act`;
            my @TMP = split( /, /, $cmd );

            # add the deliver act back to @D_ACTS, continue parsing
            foreach my $item (@TMP) {
                $item = $item . "\@$PVOB";
            }
            push( @D_ACTS, @TMP );
        }    # parse deliver activity - end
    }

    undef %saw;
    @CR_UNIQUE = grep( !$saw{$_}++, @CRS );
    print "# CR_UNIQUE=@CR_UNIQUE\n" if $debug;

    ############# this block is not in use, later should be merged into bccpbmod::getUniqueCR
   # print "#  Retrieves, removes the duplicates of the elements for the merge..\n";
   # foreach $activity ( @CR_UNIQUE ) {
   # print "# CR_UNIQUE: activity=$activity\n" if $debug;
   #
   # # Skip cancelled activity
   # $entity = $CQsession->GetEntity("BaseCMActivity", $activity);
   # $state = $entity->GetFieldValue("State")->GetValue();
   # print "# state=$state\n" if $debug;
   # next if ($state eq "Cancelled");
   #
   # $cmd = `cleartool describe -fmt "%[versions]Cp" activity:$activity\@$PVOB`;
   # @VERSIONS = split(/, /, $cmd);
   # foreach $version ( @VERSIONS ) {
   # print "# CR_UNIQUE: Delivered Version = $version\n" if $debug;
   # $version =~ /(.+)\@\@.+/;
   # if ( ! -d $1 ) {
   # unshift(@ELEMENTS, "$1");
   # }else {
   # # Check if delete activity
   # $dir = $1;
   # chdir ($dir);
   # $hdl = `cleartool describe -fmt %[headline]p activity:$activity\@$PVOB`;
   # #print "# hdl=$hdl\n" if $debug;
   # if ( uc($hdl) =~ /DELETE/) {  # must include a word Delete in headline
   # print "# Directory change on: $dir\n" if $debug;
   # $comment = `cleartool desc -fmt %c $version`;
   # @files = split(/\n/, $comment);
   # foreach $file ( @files ) {
   # $file =~ /.+\"(.*)\".+/;
   # #print "# file=$1\n" if $debug;
   # $dfile = $dir . "\\" . $1;
   # #print "# dfile:$dfile\n" if $debug;
   # unshift(@DFILES, "$dfile");
   # }
   # }
   # }
   # }
   # }
   # undef %saw;
   # @UNIQUE = grep(!$saw{$_}++, @DFILES); # not in use
    ############# this block is not in use, later should be merged into bccpbmod::getUniqueCR

    return @CR_UNIQUE;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< getEnvironmentVar >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# getEnvironmentVar returns the attribute values attached to a CC UCM stream. The
# stream used is the one on the context of the current operation.

sub getEnvironmentVar {
    my ($stream) = @_;    # get the stream which may be passed as argument
    my @zccenv;
    print "\n*** bccpbmod::getEnvironmentVar ***\n" if $debug;

    # Identify the CC View context of the operation
    if ( $my_view eq "" ) {
        $my_view = `cleartool lsview -cview -short`;
        if ( $my_view eq "" ) {
            die "\nCan't get view. Aborting...\n";
        }
    }
    chomp $my_view;
    print "\nview:$my_view\n" if $debug;

   # Identify the CC Stream context of the operation or used the input parameter
    if ( $stream eq "" ) {
        $stream = `cleartool lsstream -fmt \"%[name]Xp\" -view $my_view`;
        if ( $stream eq "" ) {
            die "\nCan't get stream. Aborting...\n";
        }
    }
    print "\nstream=$stream\n" if $debug;
    chomp $stream;

    # Get the project name
    if ( $project eq "" ) {
        $project = `cleartool lsproject -fmt \"%[name]p\" -view $my_view`;
        if ( $project eq "" ) {
            die "\nCan't get project. Aborting...\n";
        }
    }
    chomp $project;
    print "\nproject=$project\n" if $debug;    # "ZSP"

    # hl1 = first letter of the project name
    $hl1 = substr( $project, 0, 1 );
    print "\nhl1=$hl1\n" if $debug;            # "Z"

    # Get the list of CC attributes attached to the stream
    my $ret = `cleartool desc -fmt \"%a\" $stream`;
    chomp $ret;                                # remove the return char
    chop $ret;                                 # remove the closing parenthesis
    print "\nret=$ret\n";                      #

    $ret = substr( $ret, 1 );                  # remove the opening parenthesis
    my @attrs = split /, /, $ret;  # split the line into multiple lines of a='N'
    my ( $pair, $name, $value );
    my %zccenv;    # associative array for attribute name-value pairs
    my $stream_attr;

    # foreach line, a='N', split out the name and value
    print "\nStream attributes\n" if $debug;
    foreach $pair (@attrs) {
        # print "\npair is: $pair\n";  #
        ( $name, $value ) = split /=/, $pair, 2;
        # print "\nname=$name  value=$value\n";  #
        $value =~ s/\"//g;               # remove double quotes
        $value =~ s/^\$\{hl1\}/$hl1/;    # replace variables
        $value =~ s/^\$\{hl2\}/$hl2/;

        # only knows in bldCobol.pl
        #$value =~ s/^\$\{hl3\}/$hl3/;
        #$value =~ s/^\$\{hl4\}/$hl4/;
        #$value =~ s/^\$\{hl5\}/$hl5/;

        $value =~ s/\$\{pname\}/$project/;    #ZSP
             #$value =~ s/*\$\{dadsver\}*/$dadsver/; #DADSVER in envs.txt
             #$value =~ s/*\$\{cicsregions\}*/$cicsregions/; #CICSREGIONS

        $zccenv{$name} = $value;    # assign name-value pair to attributes array
                                    #print "\n$name: $value" if $debug;
        $stream_attr = $stream_attr . "$name:$value\n";
    }
    $zccenv{"PName"} = $project;

    # Return the zccenv associative array of attributes.
    return %zccenv;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< uploadFile >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# upload a file from client side to the server
sub uploadFile {
    my (
        $mf_id,   $mf_hostname, $mf_port, $server_pds,
        $bcl_dir, $input_file,  $ext
    ) = @_;
    $bcl = "
//* ------------------------------------------------------------------
//*        ---- IEFBR14 - UPLOAD FILES
//* ------------------------------------------------------------------
//*
//UPLDFL  EXEC PGM=IEFBR14
//UPLD1   DD DISP=SHR,DSN=$server_pds,
//           RCCEXT=$ext
//
";
    $bcl =~ s/\n+/\n/g;
    if ( !$single_bcl ) {
        $step = "UPLOADFILE";
        bccpbmod::writeFile( "$step.bcl", $bcl );
        $status =
          bccpbmod::exec_step( $mf_id, $mf_hostname, $mf_port, $step, $bcl_dir,
            "$step.bcl", "t ${input_file}", "" );
    }
    else {
        $bclfile = "UPLOADFILE.bcl";
        bccpbmod::writeFile( $bclfile, $bcl );
    }
    return $status;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< writeFile >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# writeFile formats and writes a text deck to a file

sub writeFile {
    my ( $outputfilename, $text ) = @_;
    print "\n*** bccpbmod::writeFile ***\n"     if $debug;
    print "\nOutput to file: $outputfilename\n" if $debug;
    open( FOUT, ">>$outputfilename" ) or die 'Unable to open output file';
    my $n = length($text);
    my $first = substr( $text, 0, 1 );
    my $last;
    if ( $n > 2 ) {
        $last = substr( $text, $n - 2, 2 );
        if ( $last eq "\n\n" ) {
            chop($text);
        }
    }
    if ( $first eq "\n" ) {
        $text = substr( $text, 1 );
    }
    print FOUT $text;
    print "\n$text\n" if $debug;
    close(FOUT);
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< devPrint >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# devPrint builds an identification string from the CC version information
# that is embedded into the file.  This information can be used to trace
# executables back to source code and deployment.

sub devPrint {
    my ( $env, $mem, $mfid, $filename ) = @_;
    my $leaf = `cleartool desc -fmt \"%Ln\" $filename`;
    print "\n*** bccpbmod::devPrint ***\n" if $debug;
    print "\nLeaf name: $leaf\n"           if $debug;
    $id = "$env.$mem.$mfid";
    print "ID: $id\n" if $debug;
    return ($id);
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< getTable >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
# To get values from DB2 and CICS table
# In the caller, get value like this:
# $value = $db2table->{"DB2LOAD"}{"DB2E"};
#
sub getTable {
    my ($file) = @_;
    my @excel;
    open( FH, $file ) or die "Can't open $file file: $!\n";
    while ( $line = <FH> ) {
        chomp($line);
        my @cells = split( /\t/, $line );
        push @excel, \@cells;
    }
    close FH;

    my $rows = $#excel;
    my $cols = $#{ $excel[0] };
    print("rows=$rows :: cols=$cols\n");

    my $table = {};    # create a reference to an anonymous hash

    for ( $row = 0 ; $row <= $rows ; $row++ ) {
        for ( $column = 0 ; $column <= $cols ; $column++ ) {

            #print $excel[$row][$column], "\t" if $debug;
            $table->{ $excel[$row][0] }{ $excel[0][$column] } =
              $excel[$row][$column];
        }

        #print "\n" if $debug;
    }

    #print $table->{"DB2LOAD"}{"DB2E"};
    print "\n" if $debug;
    return $table;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< getTypes >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# getTypes retrieves the attributes from the element in name/value pairs and returns
# an array where the array element key is the attribute name which contains the value.

sub getTypes {
    my ($filename) = @_;    # get the filename which is passed as argument
    print "\n*** bccpbmod::getTypes ***\n" if $debug;

    # Get the list of CC attributes attached to the file element -  not version
    my $ret = `cleartool describe -fmt \"%a\" -pname $filename\@\@`;
    print "\nElement: $filename\n" if $debug;
    chomp $ret;             # remove the return char
    chop $ret;              # remove the closing parenthesis
    $ret = substr( $ret, 1 );    # remove the opening parenthesis
    my @attrs = split /, /, $ret;  # split the line into multiple lines of a="N"

    # Get the list of CC attributes attached to the file type element
    my $eltype = `cleartool describe -fmt \"%[type]p\" -pname $filename\@\@`;
    chomp $eltype;                 # remove the return char
    my $ret = `cleartool describe -fmt \"%a\" eltype:$eltype@\\Mainframe_PVOB`;
    chomp $ret;                    # remove the return char
    chop $ret;                     # remove the closing parenthesis
    $ret = substr( $ret, 1 );      # remove the opening parenthesis
    my @nattrs = split /, /, $ret; # split the line into multiple lines of a="N"
    push( @attrs, @nattrs );
    push( @attrs, "TYPE=\"$eltype\"" );

    my ( $pair, $name, $value );
    my %CCattr;    # associative array for attribute name-value pairs

    # foreach line, a='N', split out the name and value
    foreach $pair (@attrs) {
        ( $name, $value ) = split /=/, $pair;
        $value =~ s/\"//g;    # remove double quotes
        $CCattr{$name} = $value;    # assign name-value pair to attributes array
                                    #print "\n$name: $value"  if $debug;
    }

    # Return a type associative array of attributes.
    return %CCattr;
}

# Sort the stream in DEV,TST,PAT,QEM sequence,fix in v8
sub sort_streams {
    my @stream_before = @_;
    @DEV = grep( /DEV/, @stream_before );
    @TST = grep( /TST/, @stream_before );
    @PAT = grep( /PAT/, @stream_before );
    @QEM = grep( /QEM/, @stream_before );
    print "\@DEV=@DEV\n";
    print "\@TST=@TST\n";
    print "\@PAT=@PAT\n";
    print "\@QEM=@QEM\n";
    my @stream_after = ( @PAT, @QEM, @TST, @DEV );
    return @stream_after;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< testServer >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# testServer performs a specific rccbuild command that verifies communication
# with the mainframe.

sub testServer {
    if ( !$preview ) {
        print "\n*** bccpbmod::testServer ***\n" if $debug;
        my ( $mf_hostname, $mf_port ) = @_;
        print "\nTesting if the server is up...\n" if $debug;
        my $cmd = "$rccbuild -h $mf_hostname\@$mf_port -testServer";
        print "Executing: $cmd\n" if $debug;
        my $status = 0;
        $status = rcc_system($cmd);
        print "\nTesting server return status is: $status\n" if $debug;

        if ( $status == 256 ) {
            print "\nUnable to connect to server $mf_hostname\@$mf_port. Aborting...\n";
        }
        return $status;
    }
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< cleanup >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# cleanup removes the BCL directory from a previous execution

sub cleanup {
    print "\n*** bccpbmod::cleanup ***\n" if $debug;
    my ($logdir) = @_;
    if ( -d $logdir ) {
        print "\nLog directory is being removed: $logdir\n" if $debug;
        rmtree( $logdir, 0, 0 );
    }
    print "\nLog and BCL directories are being created: $logdir\\BCL\n"
      if $debug;
    mkdir $logdir;
    mkdir "$logdir\\BCL";
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< rcc_system >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# rcc_system executes an rcc command on the mainframe
# Returns the status of system call

sub rcc_system {
    if ( ( !$preview ) && ( !$single_bcl ) ) {
        print "\n*** bccpbmod::rcc_system ***\n\n" if $debug;
        my ($cmd) = @_;

        my $rcc_out = "rcc.out";
        $cmd .= " > $rcc_out";
        $status = system($cmd);

        #      if ( $status != 0 ) {
        #         $status = 256;
        #      }

        #############################
        open( RCCOUT, $rcc_out ) or die "\nCan't open $rcc_out: $!\n";
        while (<RCCOUT>) {
            print STDERR if /^The MVS step.*return code is/;

            #print if /^The MVS step.*return code is/;
        }
        close RCCOUT;
        unlink($rcc_out);
        #############################
        return $status;
    }
    else {
        $status = "Preview";
    }
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< exec_step >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Execute the above step and check return code, the rccbuild command
# executes the step
# Returns the status of system call

sub exec_step {
    my ( $mf_id, $mf_hostname, $mf_port, $server_build_script, $bcl_dir,
        $bcl_file, $input_file, $output_file )
      = @_;

#  $status = bccpbmod::exec_step( $mfid, $mf_hostname, $mf_port, $step, $bcldir, "$step.bcl", "t ${file}", "" );
    print "\n*** bccpbmod::exec_step ***\n";

    # Checks if there is an input file
    if ( $input_file ne '' ) {
        $input_file = "-i$input_file";
    }

    # Checks if there is an output file
    if ( $output_file ne '' ) {
        $output_file = "-o$output_file";
    }

    # Set the server authentication mode
    if ( $auth_level == 0 ) {
        $auth = "";    # No user authentication
    }
    elsif ( $auth_level == 1 ) {
        $auth =
          "";    # The user ID and password, passed by the client, are optional
    }
    elsif ( ( $auth_level == 2 ) || ( $auth_level eq "S" ) ) {
        $auth = "-au $mf_id"
          ;      # The user id and password, passed by the client, are required
    }

    # Execute the step and print the return code
    $status = 0;
    $cmd =
"$bccpbmod::rccbuild -h $mf_hostname\@$mf_port -b $server_build_script -ft $bcl_file $input_file $output_file -n 4 -c LE $auth -k IBM-1252 -r IBM-1047 -ke ISO-8859-1 $rccVerbosity";
    print "\nStep: $server_build_script\ncmd: $cmd\n ";
    $status = bccpbmod::rcc_system($cmd);
    print
      "$server_build_script BCL system command return status was: $status\n";
    move( $bcl_file, "./BCL/$bcl_file" );
    return $status;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< exec_step_8 >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Execute the above step and check return code, the rccbuild command
# Allows a rc =8 as acceptable
# executes the step
# Returns the status of system call
sub exec_step_8 {
    my ( $mf_id, $mf_hostname, $mf_port, $server_build_script, $bcl_dir,
        $bcl_file, $input_file, $output_file )
      = @_;

#  $status = bccpbmod::exec_step( $mfid, $mf_hostname, $mf_port, $step, $bcldir, "$step.bcl", "t ${file}", "" );
    print "\n*** bccpbmod::exec_step ***\n";

    # Checks if there is an input file
    if ( $input_file ne '' ) {
        $input_file = "-i$input_file";
    }

    # Checks if there is an output file
    if ( $output_file ne '' ) {
        $output_file = "-o$output_file";
    }

    # Set the server authentication mode
    if ( $auth_level == 0 ) {
        $auth = "";    # No user authentication
    }
    elsif ( $auth_level == 1 ) {
        $auth =
          "";    # The user ID and password, passed by the client, are optional
    }
    elsif ( ( $auth_level == 2 ) || ( $auth_level eq "S" ) ) {
        $auth = "-au $mf_id"
          ;      # The user id and password, passed by the client, are required
    }

    # Execute the step and print the return code
    $status = 0;
    $cmd =
"$bccpbmod::rccbuild -h $mf_hostname\@$mf_port -b $server_build_script -ft $bcl_file $input_file $output_file -n 8 -c LE $auth -k IBM-1252 -r IBM-1047 -ke ISO-8859-1 $rccVerbosity";
    print "\nStep: $server_build_script\ncmd: $cmd\n ";
    $status = bccpbmod::rcc_system($cmd);
    print
      "$server_build_script BCL system command return status was: $status\n";
    move( $bcl_file, "./BCL/$bcl_file" );
    return $status;
}

sub exec_step_svr {

    # Execute server side script, not in use
    my ( $mf_id, $mf_hostname, $mf_port, $proclib, $server_build_script,
        $input_file, $output_file, $env_values )
      = @_;
    print "\n*** bccpbmod::exec_step ***\n";

    # Checks if there is an input file
    if ( $input_file ne '' ) {
        $input_file = "-i$input_file";
    }

    # Checks if there is an output file
    if ( $output_file ne '' ) {
        $output_file = "-o$output_file";
    }

    # Set the server authentication mode
    if ( $auth_level == 0 ) {
        $auth = "";    # No user authentication
    }
    elsif ( $auth_level == 1 ) {
        $auth =
          "";    # The user ID and password, passed by the client, are optional
    }
    elsif ( ( $auth_level == 2 ) || ( $auth_level eq "S" ) ) {
        $auth = "-au $mf_id"
          ;      # The user id and password, passed by the client, are required
    }

    # Execute the step and print the return code
    $status = 0;
    $cmd =
"$bccpbmod::rccbuild -h $mf_hostname\@$mf_port -proclib $proclib -b $server_build_script $input_file $output_file -n 4 -c LE $auth -k IBM-1252 -r IBM-1047 -ke ISO-8859-1 $rccVerbosity -v $env_values";
    print "\nStep: $server_build_script\ncmd: $cmd\n ";
    $status = bccpbmod::rcc_system($cmd);
    print "$server_build_script status was: $status\n";
    move( $bcl_file, "./BCL/$bcl_file" );
    return $status;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< pause >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# pause waits for the user to press any key

sub pause {
    print "\n*** bccpbmod::pause ***\n" if $debug;
    if ( $clearprompt = 1 ) {
        print "\nPress enter to exit.\n";
        my $ignore = <STDIN>;
    }
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< printArray >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# printArray the values of an array

sub printArray {
    print "\n*** bccpbmod::printArray ***\n" if $debug;
    foreach $name (@_) {
        print "\n$name\n";
    }
    return 0;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< my_die >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# my_die exits a script printing the error message that caused the error to stdout

sub my_die {
    print "\n*** bccpbmod::my_die ***\n" if $debug;
    my ( $error, $log1, $log2 ) = @_;
    @logs = ( $log1, $log2 );
    $current_dir = `cd`;
    chomp($current_dir);
    foreach $log (@logs) {
        $pid = fork();
        if ($pid) {

            # parent
        }
        elsif ( $pid == 0 ) {

            # child
            if ( ( $log ne "" ) && ( -e "$current_dir\\$log" ) ) {
                $cmd = system("notepad $current_dir\\$log");
                exit 0;
            }
        }
        else {
            die "Could not fork: $!\n";
        }
    }
    die "\nFATAL ERROR: $error\n";
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< vbrunas >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Run a clearcase command as Vobadm

sub vbrunas {
    print "\n*** bccpbmod::vbrunas ***\n" if $debug;
    unlink("vbrunas.vbs");
    my ($command) = @_;
    $domain = $ENV{USERDOMAIN};
    $tstamp = strftime "%H%M%S\n", localtime;
    chomp($tstamp);
    $vbs = "
On Error Resume Next
dim WshShell,oArgs,FSO

set oArgs=wscript.Arguments

sUser = oArgs(0)
sPass = oArgs(1)&VBCRLF
sCmd  = oArgs(2)

set WshShell = CreateObject(\"WScript.Shell\")
set WshEnv   = WshShell.Environment(\"Process\")
WinPath = WshEnv(\"SystemRoot\")&\"\\System32\\runas.exe\"
set FSO = CreateObject(\"Scripting.FileSystemObject\")

rc=WshShell.Run(\"runas /user:$domain\\\" & sUser & \" \" & CHR(34) & sCmd & CHR(34), 2, FALSE)
Wscript.Sleep 1000
WshShell.AppActivate(WinPath)
WshShell.SendKeys sPass

set WshShell = Nothing
set oArgs    = Nothing
set WshEnv   = Nothing
set FSO      = Nothing

wscript.quit
";
    $temp_dir = $ENV{TEMP};
    bccpbmod::writeFile( "$temp_dir\\vbrunas_${tstamp}.vbs", $vbs );
    print "$temp_dir\\vbrunas_${tstamp}.vbs $vb_user $vb_pw \"$command\"\n"
      if $debug;
    $status =
      system("$temp_dir\\vbrunas_${tstamp}.vbs $vb_user $vb_pw \"$command\"");
    unlink("$temp_dir\\vbrunas_${tstamp}.vbs");

}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< find_dir >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Finds a specific directory in a view

sub find_dir {
    my ($dir) = @_;
    print "\n*** bccpbmod::find_dir ***\n" if $debug;
    $path_name = "";
    $temp      = `cd`;
    chomp($temp);
    $temp =~ /(.+)\\.*?$/;
    $current_path = $1;
    $cview        = `cleartool pwv -root`;
    chomp($cview);

    if ( $cview eq "" ) {
        $current_path =~ /^(.+):(.+)/;
        $current_dir = "$1:";
        $search_path = $2;
    }
    else {
        $current_dir = $cview;
        $dlength     = length($current_path);
        $vlength     = length($cview);
        $search_path = substr( $current_path, $vlength, $dlength );
    }
    $search_path =~ /^\\.*?\\(.+)/;
    if ( $search_path eq "" ) {
        $dir_name = "$current_dir\\$dir";
    }
    else {
        $dir_name = "\\$dir";
    }
    print "\nSearching for: $dir_name\n\n" if $debug;

    #$path_name = $current_dir . $dir_name;
    #print "Found at: $path_name\n" if $debug;
    #@DIRS = split (/\n/,$path_name);

    $dir_name =~ s/\\/\\\\/g;
    @path_names =
      `cleartool find -avobs -cview -type d -kind delem -nxname -print`;
    chomp(@path_names);
    @TMP  = grep( /$dir_name/, @path_names );
    @TEMP = grep( s/@@//,      @TMP );
    if ( $cview eq "" ) {
        @DIRS = grep( s/^\\/$current_dir\\/, @TEMP );
    }
    else {
        @DIRS = @TEMP;
    }
    foreach $dir (@DIRS) {
        print "Found at: $dir\n" if $debug;
    }

    return \@DIRS;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<< return_env >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Return the development or production environment names

sub return_env {

    my ($env_type) = @_;

    print "\n*** bccpbmod::return_env ***\n" if $debug;

    # Retrieves the envs.txt file location
    $file = "$scriptdir\\$envs_file";

    # Reads envs.txt and retrieves the environment definitions
    open( ZCCENV, $file ) or die "Can't open envs.txt file [$file]: $!\n";
    while ( $line = <ZCCENV> ) {
        chomp($line);
        my @cells = split( /\t/, $line );
        push @zccenv, \@cells;
    }
    close ZCCENV;
    $zlines = $#zccenv;
    $zcols  = $#{ $zccenv[0] };

    # Looks for ENV and STAGE inside envs.txt and retrieves the their lines
    for ( $index1 = 0 ; $index1 <= $zlines ; $index1++ ) {
        if ( uc $zccenv[$index1][0] eq "ENV" ) {
            $env_line = $index1;

            #print "env_line : $env_line\n" if $debug;
        }
        elsif ( uc $zccenv[$index1][0] eq "STAGE" ) {
            $stage_line = $index1;

            #print "stage_line : $stage_line\n" if $debug;
        }
    }

    # Reads envs.txt and retrieves the top/production and bottom/development
    # environments
    $found = 0;
    for ( $index2 = 1 ; $index2 <= $zcols ; $index2++ ) {
        for ( $index3 = 1 ; $index3 <= $zcols ; $index3++ ) {
            if ( uc $zccenv[$stage_line][$index2] eq "" ) {
                $TOP_ENV = $zccenv[$env_line][$index2];
                $found   = 1;
            }
            elsif ( ( uc $zccenv[$env_line][$index2] ) eq
                ( uc $zccenv[$stage_line][$index3] ) )
            {
                $found = 1;
            }
        }
        if ( !$found ) {
            unshift( @ENVS, "$zccenv[$env_line][$index2]" );
        }
        $found = 0;
    }
    if ( $env_type eq "PRD" ) {
        return $TOP_ENV;
    }
    elsif ( $env_type eq "DEV" ) {
        @SORTED = sort @ENVS;
        return \@SORTED;
    }
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< top_env >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Retrieves the top/production environment project and stream

sub top_env {

    my ( $PVOB, $prod_env ) = @_;

    print "\n*** bccpbmod::top_env ***\n\n" if $debug;

    @streams = `cleartool lsstream -s -invob $PVOB`;
    foreach $stream (@streams) {
        if ( $stream =~ /(_$prod_env)$/m ) {
            chomp $stream;
            $pstream = "$stream\@$PVOB";
        }
    }
    $cmd      = `cleartool describe -fmt %[project]p stream:$pstream`;
    $pproject = "$cmd\@$PVOB";
    $pproject =~ /(.+)\@(.+)/;
    $project = $1;
    print "Production project = $pproject\n" if $debug;
    print "Production stream = $pstream\n"   if $debug;

    return ( $pproject, $pstream );
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<< view_tag >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Retrieves the top/production environment view name and its type

sub view_tag {

    my ($stream) = @_;

    $pcid  = "$ENV{USERDOMAIN}\\$ENV{USERNAME}";
    $cmd   = `cleartool describe -fmt %[views]p stream:$stream`;
    @views = split( / /, $cmd );
    foreach $view (@views) {
        $cmd = `cleartool lsview -long $view`;
        $cmd =~ /^View owner: (.*)/m;
        $view_owner = $1;
        $cmd =~ /^View attributes: (.*)/m;
        $view_ucm = $1;
        if ( ( $view_owner eq $pcid ) && ( $view_ucm =~ /ucmview/i ) ) {
            $view_tag = $view;
            if ( $view_ucm eq "ucmview" ) {
                $view_kind = "dynamic";
                $cmd       = `cleartool startview $view`;
                $view_tag  = $view;
                print "View tag = $view_tag\n" if $debug;
                last;
            }
            $view_kind = "snapshot";
        }
    }
    return ( $view_tag, $view_kind );
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< MVFS_dir >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Returns the MVFS directory

sub MVFS_dir {
    $shares  = `net use`;
    @lines   = split( /\n/, $shares );
    @results = grep /\\\\$network_name\s/, @lines;
    $result  = "@results";
    $result =~ /.+\s(.+):.+/;
    return $1;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< Find_Env >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Returns the environment from the attribute defined on PVOB: PROD or LAB.

sub find_env {
    $cmd = `cleartool describe -fmt %[ENV]Sa vob:$PVOB`;
    $cmd =~ /.+\"(.+)\".+/;
    return $1;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<< control_do >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Adds to source control or creates a new version of build derived object

sub control_do {

    my ( $env, $file_dir, $mem, $ext, $target_dir, $pattern, $type ) = @_;

    print "\n*** bccpbmod::control_do ***\n" if $debug;

    $dev_env = bccpbmod::return_env("DEV");
    if ( !( grep { $_ eq $env } @$dev_env ) ) {
        $source = "$file_dir\\$mem\\${mem}.${ext}";
        $dirs   = bccpbmod::find_dir($target_dir);
        foreach $dir (@$dirs) {
            if ( $dir =~ /$pattern/gi ) {
                $target = "$dir\\${mem}.${ext}";
                if ( -e "$target" ) {
                    print "\nChecking out $target\n" if $debug;
                    $cmd = `cleartool co -nc $target` if ( !$preview );
                    print "Copying $source to $target\n" if $debug;
                    copy( $source, $target )
                      or die "File cannot be copied."
                      if ( !$preview );
                }
                else {
                    print "\nChecking out parent directory $dir\n" if $debug;
                    $cmd = `cleartool co -nc $dir` if ( !$preview );
                    print "Copying $source to $target\n" if $debug;
                    copy( $source, $target )
                      or die "File cannot be copied."
                      if ( !$preview );
                    print "Adding to source control $target of type $type\n"
                      if $debug;
                    $cmd = `cleartool mkelem -ci -c \"${type}.\" $target`
                      if ( !$preview );
                }
            }
        }
    }
    return 0;
}

# <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< mkload >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
#
# Add to source control at production level loads and new elements, then
# rebases all dependent streams except the development ones

sub mkload {

    my ( $sstream, $pproject, $pview_kind, $prod_view, $mem, $ext, $target_dir )
      = @_;

    $pproject =~ /(.+)\@(.+)/;
    $project = $1;
    $PVOB    = $2;
    $sstream =~ /(.+)\@.+/;
    $stream = $1;

    $MVFS_dir = bccpbmod::MVFS_dir;

    $path_name = "${MVFS_dir}:\\${prod_view}\\${target_dir}";
    print "Path name: $path_name\n" if $debug;

    $target = "$path_name\\${mem}.${ext}";
    print "Target: $target\n";
    if ( $pview_kind eq "snapshot" ) {
        $cmd =
          `cleartool update -add_loadrules -force -rename -log NUL $target`;
    }
    if ( -e "$target" ) {
        print "Target $target found...\n";
        $cmd = `cleartool ls -long $target`;
        if ( $cmd !~ /view private object/ ) {
            $target = "";
            return $target;
        }
    }
    print "Creating $target\n" if $debug;
    open( LOADFILE, "> $target" ) or die "File cannot be copied.";
    close(LOADFILE);
    return $target;
}

sub getName {
    my ($mfid) = @_;
    $tstamp = strftime "%H%M%S\n", localtime;
    chomp($tstamp);
    $prefix = "";
    $name   = $mfid;
    $affix  = ".T$tstamp";
    if ( ( $envs{'HOSTPDS_PREFIX'} ne '' ) || ( $envs{'HOSTPDS_AFFIX'} ne '' ) )
    {
        if ( $envs{'HOSTPDS_PREFIX'} ne '' ) {
            $prefix = $envs{'HOSTPDS_PREFIX'} . ".";
        }
        if ( $envs{'HOSTPDS_AFFIX'} ne '' ) {
            $affix = "." . $envs{'HOSTPDS_AFFIX'};
        }

        #  this creates a temporary file on mainframe for the builds
        $name = $prefix . "TEMP." . $project_name . "." . $mfid;
    }
    return $name;
}

sub cmdCall {
    my %args = (
        'output' => "output",
        @_,
    );
    my $type = ref( $args{'cmd'} )
      or fatal("Reference to command not passed to cmdCall");
    local ($_);

    if ( $args{'output'} =~ /status/o ) {
        system("${$args{'cmd'}}");
        $? / 256;
    }
    else {
        $_ = `${$args{'cmd'}}`;
        chomp($_);
        if (wantarray) {
            split("\n");
        }
        else {
            $_;
        }
    }
}

sub cleartool {
    my %args = (
        'output' => "output",
        @_,
    );
    my $type = ref( $args{'cmd'} )
      or fatal("Reference to cleartool command not passed to ClearTool");
    local ($_);

    if ( $args{'output'} =~ /status/o ) {
        system("cleartool ${$args{'cmd'}}");
        $? / 256;
    }
    else {
        $_ = `cleartool ${$args{'cmd'}}`;
        chomp($_);
        if (wantarray) {
            split("\n");
        }
        else {
            $_;
        }
    }
}

#----------------------------------------------------
#  clearprompt with message and proceed
#----------------------------------------------------

sub message {
    my ($message) = @_;
    if ($clearprompt) {
        print "$message\n" if $debug;
        $cmd =
          "clearprompt proceed -mask proceed -prompt \"$message\" -prefer_gui";
        if ( cmdCall( cmd => \$cmd, output => 'status' ) ) {
            die "System call $cmd failed.\n";
        }
    }
    else {
        print "$message\n" if $debug;
    }
    exit(0);
}

#----------------------------------------------------
#  clearprompt with message and proceed
#----------------------------------------------------

sub choice {
    my ($message) = @_;
    if ($clearprompt) {
        print "$message\n" if $debug;
        $cmd =
"clearprompt yes_no -default no -mask yes,no -prompt \"$message\" -prefer_gui";
        if ( cmdCall( cmd => \$cmd, output => 'status' ) ) {
            die "System call $cmd failed.\n";
        }
    }
    else {
        print "$message\n" if $debug;
    }
    exit(0);
}

#----------------------------------------------------
#  Fatal error, clearprompt with message and exit
#----------------------------------------------------

sub fatal {
    my ($message) = @_;
    if ($clearprompt) {
        print "$message\n" if $debug;
        $cmd =
"clearprompt proceed -type error -default abort -mask abort -prompt \"$message\" -prefer_gui";
        if ( cmdCall( cmd => \$cmd, output => 'status' ) ) {
            die "System call $cmd failed.\n";
        }
    }
    else {
        print "$message\n" if $debug;
    }
    exit(1);
}

print "\n***** finish bccpbmod.pl *****\n\n" if $debug;

1;

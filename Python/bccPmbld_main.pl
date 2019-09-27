################################################################################
# 1. bldCobol.pl returns 0 if success, or die with no returun.
# 2. bccPmbld_main.pl checks the above return and print a message, return not defined.
################################################################################

print "\n***** start bccPmbld_main.pl *****\n\n" if $debug;

use File::Basename;
use Getopt::Long;
use bccpbmod;

# #############################################################################
#
# Usage and input parameters
#
$SYNOPSIS = "ccperl bccPmbld_main.pl [-h] [-b] [-p] [-r] [-l] [-e] [-s env] -d dir file ...";
$ARGUMENTS = "
 -h        Displays this message and exits.
 -l        Identifies the link job to be submitted.
 -b        Identifies the backout job to be submitted.
 -p        DB2 package bind.
 -r        Delete the member.
 -e        Export the member.
 -s env    Identifies the source environment for a code promote.
 -d dir    Identifies the directory where the elements are located.
 file ...  Identifies the file or list of files to be compiled.\n";

$return_status = GetOptions( "h", "l", "b", "p", "r", "e", "s:s", "d:s" );
usage(1) if ($opt_h);
usage(2) if ( !$return_status );

sub usage {
    my ($level) = @_;
    $level = 2 unless ( $level =~ /^\d+$/o );
    print "\nUsage: ${SYNOPSIS}\n";
    print "\nArguments:\n${ARGUMENTS}" unless ( $level > 1 );
    exit($level);
}
###############################################################################

$debug     = $bccpbmod::debug;
$scriptdir = $bccpbmod::scriptdir;

$file_dir = $opt_d;
$link_opt = "-l" if ($opt_l);
$back_opt = "-b" if ($opt_b);
$source_opt = "-s $opt_s" if ($opt_s);
print "source_opt=$source_opt" if $debug;

$CLEARPROMPT = "$ENV{'CLEARPROMPT'}";
$CLEARPROMPT = 'BF' if ($opt_r);
print "CLEARPROMPT=$CLEARPROMPT\n" if $debug;

$op_kind = $ENV{"CLEARCASE_OP_KIND"};
print "\$op_kind=$op_kind\n" if debug;

$current_dir = `cd`;
print "current_dir=$current_dir" if $debug;

# Determine the directory context for the execution
if ( $file_dir ne '' ) {

    # directory of file specified, cd to there
    print "\nChanging directory from $current_dir to $file_dir" if $debug;
    chdir($file_dir);
    $current_dir = `cd`;
    print "\nCurrent dir = $current_dir" if $debug;
}
print "\nBuild element dir: $file_dir\n" if $debug;

if ( $file_dir =~ /OBS/ ) {
    bccpbmod::fatal("Obsolete file directory $file_dir Aborting...\n");
}
 
$no_unsupported_files = 0;
$message              = "";
$bldCobol             = "";    # COBOL build script
$bldAsm               = "";    # Assembler build script
$bldOverlay           = "";    # AFP Overlay build script
$bldPagedef           = "";    # AFP Pagedef build script
$bldEzpimu            = "";    # Easytrieve IMU build script
$bldCfg               = "";    # Vsam configuration files
$bldBind              = "";    # DB2 bind of plan and package
$bldCbc               = "";    # C++ build script
$bldEos               = "";    # EOS build script
$bldPsf               = "";    # PSF build script



$bldCobol   = $scriptdir . "\\bldCobol.pl";
$bldAsm     = $scriptdir . "\\bldAsm.pl";
$uploadcopy = $scriptdir . "\\uploadCopy.pl";
$bldEzpimu  = $scriptdir . "\\bldEzpimu.pl";
$bldCfg     = $scriptdir . "\\bldCfg.pl";
$bldBind    = $scriptdir . "\\bldBind.pl";
$bldCbc     = $scriptdir . "\\bldCbc.pl";
$bldPsf     = $scriptdir . "\\bldPsf.pl";
$bldEos     = $scriptdir . "\\bldEos.pl";

if ($debug) {
    print "\nBuild script dir: $scriptdir";
    print "\nCOBOL Build script: $bldCobol";
    print "\nAssembler Build script: $bldAsm";
    print "\nUpload script dir: $uploadcopy";
    print "\nEasytrieve IMU Build script: $bldEzpimu";
    print "\nConfiguration Vsam build script: $bldCfg";
    print "\nDB2 binds build script: $bldBind";
    print "\nC++ Build script: $bldCbc";
    print "\nPSF build script: $bldPsf";
    print "\nEOS build script: $bldEos";
}


# #############################################################################
# This process determines what language types each element is, and that will call
# the appropriate build process/subroutine/build engine for each element type.
# Please use the COBOL build script/process as an example. The last
# test determines if the LANG language is supported. The LANG variable is part of the
# element level attributes that are queried at the beginning of this process,
# These variables are assigned to the element as part of the migration to ClearCase
# or part of adding an element to source control
#
@cobol_files;
@asm_files;
@ezpimu_files;
@cfg_files;
@bnd_files;
@cbc_files;
@psf_files;
@eos_files;

foreach $path (@ARGV) {
    print "\n\nMEMBER: $path\n";
	return 0;	
    my %types = bccpbmod::getTypes($path);

    #  check type for any Cobol program
    if ( ( uc $types{'TYPE'} =~ /CII/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @cobol_files, $path );
    }

    #  check type for any cics maps
    elsif (( uc $types{'TYPE'} =~ /MAP/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @asm_files, $path );
    }

    #  check for assembler program
    elsif (( uc $types{'TYPE'} =~ /ASM/ )
        && ( uc $types{'TYPE'} !~ /COPY/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @asm_files, $path );
    }

    #  check for easytrieve with imu program
    elsif (( uc $types{'TYPE'} =~ /EZP/ )
        && ( uc $types{'TYPE'} !~ /COPY/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @ezpimu_files, $path );
    }

    #  check for C++ program
    elsif (( uc $types{'TYPE'} =~ /CBC/ )
        && ( uc $types{'TYPE'} !~ /COPY/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @cbc_files, $path );
    }

    #  check for configuration vsam items
    elsif (( uc $types{'TYPE'} =~ /CFG/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @cfg_files, $path );
    }

    #  check for DB2 Bind cards
    elsif (( uc $types{'TYPE'} =~ /BIND/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @bnd_files, $path );
    }

    # PSF
    elsif (( uc $types{'TYPE'} =~ /PSF/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @psf_files, $path );
    }

    # EOS
    elsif (( uc $types{'TYPE'} =~ /EOS/ )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @eos_files, $path );
    }
    elsif (( uc $types{'TYPE'} eq 'CLISTNOPROC' )
        || ( uc $types{'TYPE'} =~ /COPY/ )
        || ( uc $types{'TYPE'} eq 'LENOPROC' )
        || ( uc $types{'TYPE'} eq 'LECARD' )
        || ( uc $types{'TYPE'} eq 'DOCNOPROC' )
        || ( uc $types{'TYPE'} =~ /DRVR/ )
        || ( uc $types{'TYPE'} eq 'DRVRSKELNOPROC' )
        || ( uc $types{'TYPE'} eq 'PROCNOPROC' )
        || ( uc $types{'TYPE'} =~ /CNTL/ )
        || ( uc $types{'TYPE'} =~ /ESPPROC/ )
        || ( uc $types{'TYPE'} eq 'LOADNOPROC' )
        || ( uc $types{'TYPE'} eq 'LINKNOPROC' )
        || ( uc $types{'TYPE'} eq 'SRCNOPROC' )
        || ( uc $types{'TYPE'} eq 'SRCXNOPROC' )
        || ( uc $types{'TYPE'} =~ /ISPF/ )
        || ( uc $types{'TYPE'} =~ /SDF/ )
        || ( uc $types{'TYPE'} eq 'SASNOPROC' )
        || ( uc $types{'TYPE'} =~ /NETCAP/ )
        || ( uc $types{'TYPE'} =~ /UTLW/ )
        || ( uc $types{'TYPE'} =~ /FOC/ )
        || ( uc $types{'TYPE'} eq 'CULNOPROC' )
        || ( uc $types{'TYPE'} eq 'DBRMNOPROC' )
        || ( uc $types{'TYPE'} eq 'TBLNOPROC' )
        && ( uc $types{'OBSOLETE'} ne 'TRUE' ) )
    {
        push( @copy_files, $path );
    }
    else {
        print "\nUnsupported type: $types{'TYPE'}\nElement: $path\n";
        $no_unsupported_files++;
    }
}
$file = "";

# #############################################################################
#
# --------------------------- Upload Copy files -------------------------------
#
$no_copy_files = 0;
$no_copy_files = @copy_files;

if ( $no_copy_files != 0 ) {
    print "\n\nUploading files\n";
    bccpbmod::printArray(@copy_files);
    foreach $path (@copy_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n=======================================================\n";
        print "Uploading member: " . $file . "\n";

        # Exit if the file is not a valid member
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr{\..*} );
        if (   ( uc $ext ne ".SAS" )
            && ( uc $ext ne ".CPY" )
            && ( uc $ext ne ".LNK" )
            && ( uc $ext ne ".TXT" )
            && ( uc $ext ne ".DRV" )
            && ( uc $ext ne ".PRC" )
            && ( uc $ext ne ".CTL" )
            && ( uc $ext ne ".TBL" )
            && ( uc $ext ne ".UTL" )
            && ( uc $ext ne ".CUL" )
            && ( uc $ext ne ".ESP" )
            && ( uc $ext ne ".BIN" )
            && ( uc $ext ne ".SDF" )
            && ( uc $ext ne ".CLT" )
            && ( uc $ext ne ".FOC" )
            && ( uc $ext ne ".SPF" )
            && ( uc $ext ne ".SRC" )
            && ( uc $ext ne ".NET" )
            && ( uc $ext ne ".DBR" ) )
        {
            bccpbmod::fatal("File extension $ext is not correct!");
        }
        my $logfile = "";
        $logfile = $file_dir . "\\" . $file . "_uploadCopy.log";

        #$errfile = $file_dir . "\\" . $file . "_uploadCopy_error.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "upload";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $uploadcopy $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $uploadcopy $back_opt $source_opt -r -d $file_dir -f $file >> $logfile"
            );

            #$rc='000008' if the file is not there
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $uploadcopy $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );

#$rc = system ("ccperl -I$scriptdir $uploadcopy $back_opt $source_opt -d $file_dir -f $file 1>$logfile 2>$errfile");
        }
        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {

            # fatel error
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!.\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ------------------------------ Compile PSF files ----------------------------
#
#  this section needs to be done later with psfsrc; we perform actions of psf
#  at lower type level
#
$no_psf_files = 0;
$no_psf_files = @psf_files;

if ( $no_psf_files != 0 ) {
    print "\n\nBuilding PSF files:\n";
    bccpbmod::printArray(@psf_files);
    foreach $path (@psf_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Overlay source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( uc $ext ne ".PSF" ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        my $logfile = "";
        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "generation";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldPsf $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldPsf $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldPsf $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }
        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
# EOS
#
#
#
$no_eos_files = 0;
$no_eos_files = @eos_files;

if ( $no_eos_files != 0 ) {
    print "\n\nBuilding EOS files:\n";
    bccpbmod::printArray(@eos_files);
    foreach $path (@eos_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Pagedef source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if (   ( uc $ext ne ".FRM" )
            && ( uc $ext ne ".DRV" )
            && ( uc $ext ne ".FGR" )
            && ( uc $ext ne ".PGR" )
            && ( uc $ext ne ".USR" )
            && ( uc $ext ne ".UGR" )
            && ( uc $ext ne ".TBL" ) )
        {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }
        my $logfile = "";
        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "generation";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldEos $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldEos $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        elsif ($opt_e) {
            $message = "export";
            $rc      = system(
"ccperl -I$scriptdir $bldEos $back_opt $source_opt -e -d $file_dir -f $file > $logfile"
            );
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldEos $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }
        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ------------------------------ Compile MAP files ----------------------------
#
$no_map_files = 0;
$no_map_files = @map_files;

if ( $no_map_files != 0 ) {
    print "\n\nBuilding MAP files:\n";
    bccpbmod::printArray(@map_files);
    foreach $path (@map_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an CICS Map source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( uc $ext ne ".MAP" ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "build";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldCICSMap $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldCICSMap $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldCICSMap $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# -------------------------- Compile COBOL files -----------------------------
#
$no_cobol_files = 0;
$no_cobol_files = @cobol_files;

if ( $no_cobol_files != 0 ) {
    print "\n\nBuilding COBOL files\n";
    bccpbmod::printArray(@cobol_files);
    foreach $path (@cobol_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n=======================================================\n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not a COBOL source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );

#  need to confirm extension of a cobol program and a composite link of cobol program
        if ( ( uc $ext ne ".CBL" ) && ( uc $ext ne ".LNK" ) ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "build";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $message = "backout";
            $rc      = system(
"ccperl -I$scriptdir $bldCobol $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_p) {
            $message = "Build DB2 Package";
            $rc      = system(
"ccperl -I$scriptdir $bldCobol $back_opt $source_opt -p -d $file_dir -f $file > $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldCobol $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldCobol $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );

#$rc = system ("ccperl -I$scriptdir $bldCobol $back_opt $source_opt -d $file_dir -f $file");
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ------------------------------ Compile ASM files ----------------------------
#
$no_asm_files = 0;
$no_asm_files = @asm_files;

if ( $no_asm_files != 0 ) {
    print "\n\nBuilding ASM files:\n";
    bccpbmod::printArray(@asm_files);
    foreach $path (@asm_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Assembler source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( ( uc $ext ne ".ASM" ) && ( uc $ext ne ".MAP" ) ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "build";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldAsm $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldAsm $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        elsif ($opt_p) {
            $message = "Build DB2 Package";
            $rc      = system(
"ccperl -I$scriptdir $bldAsm $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldAsm $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ----------------Compile Easytrieve with IMU files ----------------------------
#
$no_ezpimu_files = 0;
$no_ezpimu_files = @ezpimu_files;

if ( $no_ezpimu_files != 0 ) {
    print "\n\nBuilding EZPIMU files:\n";
    bccpbmod::printArray(@ezpimu_files);
    foreach $path (@ezpimu_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Assembler source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( uc $ext ne ".EZP" ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";

        if ( ($opt_b) && ( -e "$logfile" ) ) {
            print "opt_b called.\n";
            $rc = system(
"ccperl -I$scriptdir $bldEzpimu $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            print "opt_r called.\n";
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldEzpimu $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        elsif ($opt_p) {
            print "opt_p called.\n";
            $message = "Build DB2 Package";
            $rc      = system(
"ccperl -I$scriptdir $bldEzpimu $back_opt $source_opt -p -d $file_dir -f $file > $logfile"
            );
        }
        else {
            print "no opt called.\n";
            $rc = system(
"ccperl -I$scriptdir $bldEzpimu $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The build of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The build of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ----------------Compile C++ (CBC) files ----------------------------
#
$no_cbc_files = 0;
$no_cbc_files = @cbc_files;

if ( $no_cbc_files != 0 ) {
    print "\n\nBuilding CBC files:\n";
    bccpbmod::printArray(@cbc_files);
    foreach $path (@cbc_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Assembler source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( uc $ext ne ".CBC" ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";

        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldCbc $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldCbc $back_opt $source_opt -r -d $file_dir -f $file >> $logfile"
            );

            #$rc='000008' if the file is not there
        }
        elsif ($opt_p) {
            $message = "Build DB2 Package";
            $rc      = system(
"ccperl -I$scriptdir $bldCbc $back_opt $source_opt -r -d $file_dir -f $file >> $logfile"
            );
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldCbc $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
            print "\$rc=$rc\n";
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The build of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The build of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ----------------Configuration VSAM items ----------------------------
#
$no_cfg_files = 0;
$no_cfg_files = @cfg_files;

if ( $no_cfg_files != 0 ) {
    print "\n\nBuilding Configuration Vsam items:\n";
    bccpbmod::printArray(@cfg_files);
    foreach $path (@cfg_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Assembler source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( uc $ext ne ".CFG" ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }

        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";

        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldCfg $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldCfg $back_opt $source_opt -r -d $file_dir -f $file >> $logfile"
            );

            #$rc='000008' if the file is not there
        }
        else {

#$rc = system ("ccperl -I$scriptdir $bldCfg $back_opt $source_opt -d $file_dir -f $file");
            $rc = system(
"ccperl -I$scriptdir $bldCfg $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The build of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The build of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# ----------------DB2 bind of plan and package  ----------------------------
#
$no_bnd_files = 0;
$no_bnd_files = @bnd_files;

if ( $no_bnd_files != 0 ) {
    print "\n\nBuilding DB2 binds for plan and package:\n";
    bccpbmod::printArray(@bnd_files);
    foreach $path (@bnd_files) {
        my ( $file, $directory, $suffix ) = fileparse($path);
        print "\n======================================================= \n";
        print "Building file: " . $file . "\n";

        # Exit if the file is not an Assembler source
        my ( $testfile, $testdir, $ext ) =
          File::Basename::fileparse( $file, qr/\.[^.]*/ );
        if ( uc $ext ne ".BND" ) {
            bccpbmod::fatal("File extension $ext is not correct!");
            exit 1;
        }
        $logfile = $file_dir . "\\" . $file . "_compile.log";
        print "\n\nLog file: " . $logfile . "\n\n";
        $message = "build";
        if ( ($opt_b) && ( -e "$logfile" ) ) {
            $rc = system(
"ccperl -I$scriptdir $bldBind $back_opt $source_opt -d $file_dir -f $file >> $logfile"
            );
        }
        elsif ($opt_r) {
            $message = "delete";
            $rc      = system(
"ccperl -I$scriptdir $bldBind $back_opt $source_opt -r -d $file_dir -f $file > $logfile"
            );

            #$rc='000008' if the file is not there
        }
        else {
            $rc = system(
"ccperl -I$scriptdir $bldBind $back_opt $source_opt -d $file_dir -f $file > $logfile"
            );
        }

        if ( $rc == 0 ) {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file was successfully completed\n";
            print "--------------------------------------------------------\n";
        }
        else {
            print
              "\n--------------------------------------------------------\n";
            print "The $message of $file failed, returned error: $!\n";
            print "Please check more details in $logfile.\n";
            print "--------------------------------------------------------\n";
        }
    }
}

# #############################################################################
#
# Wait at the end of the compilation for the user to press "any key".  This
# allows the user to review the DOS window output before the window is closed.
#

if ( $op_kind =~ /deliver/ ) {
    print "No pause in deliver\n";
}
else {
    bccpbmod::pause();
    print "Pause for developer\n";
}

print "\n***** finish bccPmbld_main.pl *****\n\n";
exit 0;

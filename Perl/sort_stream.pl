@stream = (
    "AOL_TST", "AOL_DEV2", "AOL_DEV", "AOL_TST2",
    "AOL_QEM", "AOL_PRD",  "AOL_PAT"
);

#@stream = ("AOL_TST","AOL_DEV","AOL_QEM","AOL_PRD","AOL_PAT");
print "Before \@stream=@stream\n";
@stream = sort_streams(@stream);
print "After \@stream=@stream\n";

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
    my @stream_after = ( @DEV, @TST, @PAT, @QEM );
    return @stream_after;
}

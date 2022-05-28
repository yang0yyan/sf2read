public enum SFSampleLink {
    monoSample(1),
    rightSample(2),
    leftSample(4),
    linkedSample(8),
    RomMonoSample(32769),
    RomRightSample(32770),
    RomLeftSample(32772),
    RomLinkedSample(32776);

    private int num;

    SFSampleLink(int num) {
        this.num = num;
    }
}

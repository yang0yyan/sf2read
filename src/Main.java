// midi库 https://freepats.zenvoid.org/index.html

public class Main {
    private static final String filePath = "E:\\文件\\学习\\midi\\YDP-GrandPiano-SF2-20160804\\YDP-GrandPiano-20160804.sf2";
    private static final String filePath2 = "E:\\文件\\学习\\midi\\[SF2] GM SoundFonts [shared by ZSF] - Crisis GM 3.51 ZSF Edit.sf2";

    private static final String filePathSF2 = "E:\\文件\\学习\\midi\\midi声音库\\FreePatsGM-20210329.sf2";


    private static final String midFile1 = "E:\\文件\\学习\\midi\\铃儿响叮当.mid ";
    private static final String midFile2 = "E:\\文件\\学习\\midi\\蓝色的多瑙河.mid ";

    public static void main(String[] args) {
//        SoundFontRead sfr = new SoundFontRead();
//        sfr.getFileStream(filePath, "");
        MidiRead mr = new MidiRead();
        mr.getFileStream(midFile1,"");
        MidiPlayer midiPlayer = new MidiPlayer();
        midiPlayer.setData(mr.getChunkList());
        midiPlayer.play();
    }


}

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RIFF文件格式
 * RIFF格式是一种树状的结构，其基本组成单元为LIST和CHUNK
 * LIST可以包含多个CHUNK或者多个LIST
 * <p>
 * 'RIFF'和'LIST'也是chunk,只是它的dat由两部分组成：type、restdat
 */
public class SoundFontRead {

    private long fileIndex = 0;

    private byte[] b1 = new byte[1];
    private byte[] b2 = new byte[2];
    private byte[] b4 = new byte[4];
    private RandomAccessFile mRandomAccessFile;
    private long fileLength;

    private List<String> presetNameList = new ArrayList<>();
    private List<String> instNameList = new ArrayList<>();

    public void getFileStream(String filePath, String fileName) {
        File file = new File(filePath, fileName);

        try {
            mRandomAccessFile = new RandomAccessFile(file, "r");
            readStream();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readStream() throws IOException {
        fileLength = mRandomAccessFile.length();
        readRIFF();
    }

    private void readRIFF() throws IOException {
        CList chunk = readList();
        System.out.println(chunk);
        if (!chunk.id.equals("RIFF")) {
            System.out.println("非RIFF文件");
        }
        if (chunk.size + 8 < fileLength) {
            seek(chunk.size + chunk.index);
            Chunk orherChunk = readList();
            System.out.println("额外的chunk:" + orherChunk);
        }
        seek(chunk.index + 4);
        readRIFFChild(chunk);

//        System.out.println(chunk);
        System.out.println(fileLength);
        System.out.println("预设："+presetNameList.toString());
        System.out.println("乐器："+instNameList.toString());
    }

    private void readRIFFChild(CList list) throws IOException {
        CList list2 = readList();
        System.out.println(list2);
        list.child.add(list2);
        if (list2.type.equals("INFO")) {
            readRIFFInfo(list2);
        } else if (list2.type.equals("sdta")) {
            readRIFFSdta(list2);
        } else if (list2.type.equals("pdta")) {
            readRIFFPdta(list2);
        }
        if (list.size > list2.size + list2.index - list.index) {
            seek(list2.size + list2.index);
            readRIFFChild(list);
        } else {
            if (list.size == list2.size + list2.index - list.index) {
                System.out.println("大小相符");
            } else {
                System.out.println("大小不相符");
            }
            System.out.println("读完");
        }
    }

    private void readRIFFInfo(CList list) throws IOException {
        Chunk chunk = readChunk();

        if (chunk.id.equals("ifil")) {
            chunk.describe = "Sound Font RIFF文件的版本";
            int wMajor = readUnsignedShort();
            int wMinor = readUnsignedShort();
            chunk.data = wMajor + "." + wMinor;
        } else if (chunk.id.equals("isng")) {
            chunk.describe = "Sound Font引擎";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("INAM")) {
            chunk.describe = "Sound Font乐队名";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("irom")) {
            chunk.describe = "声音ROM名称";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("iver")) {
            chunk.describe = "声音ROM版本";
            int wMajor = readUnsignedShort();
            int wMinor = readUnsignedShort();
            chunk.data = wMajor + "." + wMinor;
        } else if (chunk.id.equals("ICRD")) {
            chunk.describe = "乐队的成立日期";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("IENG")) {
            chunk.describe = "乐队的音响设计师和工程师";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("IPRD")) {
            chunk.describe = "乐队的目标产品";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("ICOP")) {
            chunk.describe = "包含任何版权信息";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("ICMT")) {
            chunk.describe = "包含对乐队的任何评论";
            chunk.data = readString((int) chunk.size);
        } else if (chunk.id.equals("ISFT")) {
            chunk.describe = "用于创建和更改乐队的SoundFont工具";
            chunk.data = readString((int) chunk.size);
        }
        list.child.add(chunk);
        System.out.println(chunk);
        if (list.size > chunk.size + chunk.index - list.index) {
            seek(chunk.size + chunk.index);
            readRIFFInfo(list);
        } else {
            if (list.size == chunk.size + chunk.index - list.index) {
                System.out.println("大小相符");
            } else {
                System.out.println("大小不相符");
            }
            System.out.println("读完");
        }
    }

    private void readRIFFSdta(CList list) throws IOException {
        Chunk chunk = readChunk();
        if (chunk.id.equals("smpl")) {
            chunk.describe = "高16位的数字音频采样";
        } else if (chunk.id.equals("sm24")) {
            chunk.describe = "低8位的数字音频采样";
        }
        list.child.add(chunk);
        System.out.println(chunk);
        if (list.size > chunk.size + chunk.index - list.index) {
            seek(chunk.size + chunk.index);
            readRIFFSdta(list);
        } else {
            if (list.size == chunk.size + chunk.index - list.index) {
                System.out.println("大小相符");
            } else {
                System.out.println("大小不相符");
            }
            System.out.println("读完");
        }
    }

    private void readRIFFPdta(CList list) throws IOException {
        Chunk chunk = readChunk();
        if (chunk.id.equals("phdr")) {
            chunk.describe = "预设的标题";
            if (chunk.size % 38 != 0)
                System.out.println("phdr大小不是38的倍数");
            int count = (int) (chunk.size / 38);
            List<Map<String, String>> list1 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, String> map = new HashMap<>();
                String name = readString(20);
                presetNameList.add(name);
                map.put("achPresetName", name); // 预设名称
                map.put("wPreset", readUnsignedShort() + ""); // MIDI预设编号
                map.put("wBank", readUnsignedShort() + ""); // 适用于此预设的 MIDI库编号
                map.put("wPresetBagNdx", readUnsignedShort() + ""); // PBAG sub-chunk子块中预设区域列表的索引
                map.put("dwLibrary", readUnsignedInt() + "");
                map.put("dwGenre", readUnsignedInt() + "");
                map.put("dwMorphology", readUnsignedInt() + "");
                list1.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list1.toString();
        } else if (chunk.id.equals("pbag")) {
            chunk.describe = "预设的索引列表";
            if (chunk.size % 4 != 0)
                System.out.println("pbag大小不是4的倍数");
            int count = (int) (chunk.size / 4);
            List<Map<String, Integer>> list2 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Integer> map = new HashMap<>();
                map.put("wGenNdx", readUnsignedShort()); // PGEN sub-chunk子块中预设发生器区域列表的索引
                map.put("wModNdx", readUnsignedShort()); // PMOD sub-chunk子块中其调制器列表的索引
                list2.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list2.toString();
        } else if (chunk.id.equals("pmod")) {
            chunk.describe = "预设调制器列表";
            if (chunk.size % 10 != 0)
                System.out.println("pmod大小不是10的倍数");
            int count = (int) (chunk.size / 10);
            List<Map<String, Integer>> list3 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Integer> map = new HashMap<>();
                map.put("sfModSrcOper", readUnsignedShort()); // 该值表示调制器的数据源
                map.put("sfModDestOper", readUnsignedShort()); // 指示调制器的终点
                map.put("modAmount", readUnsignedShort());
                map.put("sfModAmtSrcOper", readUnsignedShort());
                map.put("sfModTransOper", readUnsignedShort());
                list3.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list3.toString();
        } else if (chunk.id.equals("pgen")) {
            chunk.describe = "预设生成器列表";
            if (chunk.size % 4 != 0)
                System.out.println("pgen大小不是4的倍数");
            int count = (int) (chunk.size / 4);
            List<Map<String, Integer>> list4 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Integer> map = new HashMap<>();
                map.put("sfGenOper", readUnsignedShort());
                map.put("genAmount", readUnsignedShort());
                list4.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list4.toString();
        } else if (chunk.id.equals("inst")) {
            chunk.describe = "仪器名称和索引";
            if (chunk.size % 22 != 0)
                System.out.println("inst大小不是22的倍数");
            int count = (int) (chunk.size / 22);
            List<Map<String, String>> list5 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, String> map = new HashMap<>();
                String name = readString(20);
                instNameList.add(name);
                map.put("achInstName", name); // 用ASCII表示的乐器名称
                map.put("wInstBagNdx", readUnsignedShort() + ""); // IBAG sub-chunk子块中乐器区域列表的索引
                list5.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list5.toString();
        } else if (chunk.id.equals("ibag")) {
            chunk.describe = "仪器索引表";
            if (chunk.size % 4 != 0)
                System.out.println("ibag大小不是4的倍数");
            int count = (int) (chunk.size / 4);
            List<Map<String, Integer>> list6 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Integer> map = new HashMap<>();
                map.put("wInstGenNdx", readUnsignedShort()); // IGEN sub-chunk子块中乐器区域发生器列表的索引
                map.put("wInstModNdx", readUnsignedShort()); // IMOD sub-chunk子块中调制器列表的索引
                list6.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list6.toString();
        } else if (chunk.id.equals("imod")) { // 列出了音源兼容文件中的所有乐器区调制器
            chunk.describe = "仪表调节器列表";
            if (chunk.size % 10 != 0)
                System.out.println("imod大小不是10的倍数");
            int count = (int) (chunk.size / 10);
            List<Map<String, Integer>> list7 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Integer> map = new HashMap<>();
                map.put("sfModSrcOper", readUnsignedShort());
                map.put("sfModDestOper", readUnsignedShort());
                map.put("modAmount", readUnsignedShort());
                map.put("sfModAmtSrcOper", readUnsignedShort());
                map.put("sfModTransOper", readUnsignedShort());
                list7.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list7.toString();
        } else if (chunk.id.equals("igen")) { // ，包含SoundFont兼容文件中每个乐器区域生成器的列表
            chunk.describe = "仪器生成器列表";
            if (chunk.size % 4 != 0)
                System.out.println("igen大小不是4的倍数");
            int count = (int) (chunk.size / 4);
            List<Map<String, Integer>> list8 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, Integer> map = new HashMap<>();
                map.put("sfGenOper", readUnsignedShort());
                map.put("genAmount", readUnsignedShort());
                list8.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list8.toString();
        } else if (chunk.id.equals("shdr")) { // smpl sub-chunk子块中的所有采样和所有引用的ROM采样
            chunk.describe = "采样标题";
            if (chunk.size % 46 != 0)
                System.out.println("shdr大小不是46的倍数");
            int count = (int) (chunk.size / 46);
            List<Map<String, String>> list9 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Map<String, String> map = new HashMap<>();
                String name = readString(20);
                instNameList.add(name);
                map.put("achSampleName", name); // 用ASCII表示的采样名称
                map.put("dwStart", readUnsignedInt() + ""); // 采样数据字段开始到该采样的第一个数据点的索引
                map.put("dwEnd", readUnsignedInt() + ""); // 从采样数据字段的开头到该采样后面46个零值数据点集的第一个
                map.put("dwStartloop", readUnsignedInt() + "");
                map.put("dwEndloop", readUnsignedInt() + "");
                map.put("dwSampleRate", readUnsignedInt() + "");

                map.put("dwSampleRate", readUnsignedByte() + "");
                map.put("dwSampleRate", readUnsignedByte() + "");
                map.put("wSampleLink", readUnsignedShort() + "");
                map.put("dwSampleRate", readUnsignedShort() + "");
                list9.add(map);
                System.out.println(map.toString());
            }
            chunk.data = list9.toString();
        }
        list.child.add(chunk);
        System.out.println(chunk);
        if (list.size > chunk.size + chunk.index - list.index) {
            seek(chunk.size + chunk.index);
            readRIFFPdta(list);
        } else {
            if (list.size == chunk.size + chunk.index - list.index) {
                System.out.println("大小相符");
            } else {
                System.out.println("大小不相符");
            }
            System.out.println("读完");
        }
    }

    private Chunk readChunk() throws IOException {
        Chunk chunk = new Chunk();
        chunk.id = readString(4);
        chunk.size = readUnsignedInt();
        chunk.index = fileIndex;
        return chunk;
    }

    private CList readList() throws IOException {
        CList chunk = new CList();
        chunk.id = readString(4);
        chunk.size = readUnsignedInt();
        chunk.index = fileIndex;
        chunk.type = readString(4);
        return chunk;
    }

    private int readLen() throws IOException {
        fileIndex += 1;
        return mRandomAccessFile.read(b1);
    }

    private int read2Len() throws IOException {
        fileIndex += 2;
        return mRandomAccessFile.read(b2);
    }

    private int read4Len() throws IOException {
        fileIndex += 4;
        return mRandomAccessFile.read(b4);
    }

    // Read 8 bit unsigned integer from stream
    public int readUnsignedByte() throws IOException {
        readLen();
        return (b4[0] & 0xFF);
    }

    // Read 16 bit unsigned integer from stream
    public int readUnsignedShort() throws IOException {
        read2Len();
        return ((b2[1] & 0xFF) << 8) |
                (b2[0] & 0xFF);
    }

    // Read 32 bit unsigned integer from stream
    public long readUnsignedInt() throws IOException {
        read4Len();
        return ((b4[3] & 0xFF) << 24) |
                ((b4[2] & 0xFF) << 16) |
                ((b4[1] & 0xFF) << 8) |
                (b4[0] & 0xFF);
    }

    public String readString(int len) throws IOException {
        byte[] bytes = new byte[len];
        fileIndex += len;
        mRandomAccessFile.read(bytes);
        for(int i=0;i<bytes.length;i++){
            if(bytes[i]==0){
                bytes[i] = 0X20;
            }
        }
        return new String(bytes, "ascii");
    }

//
//    public int readInt() throws IOException {
//        read4Len();
//        int x = ((b4[3] & 0xFF) << 24) |
//                ((b4[2] & 0xFF) << 16) |
//                ((b4[1] & 0xFF) << 8) |
//                (b4[0] & 0xFF);
//        return x;
//    }
//
//    public int readUnsignedShort() throws IOException {
//        read2Len();
//        return b2[0] | (b2[1] << 8);
//    }
//    public long readUnsignedInt() throws IOException {
//        read4Len();
//        return b4[0] + (b4[1] << 8) | (b4[2] << 16) | (b4[3] << 24);
//    }
//
//    public int read2Int() throws IOException {
//        read2Len();
//        int x = ((b4[1] & 0xFF) << 8) |
//                (b4[0] & 0xFF);
//        return x;
//    }
//
//    public String readString() throws IOException {
//        read4Len();
//        return new String(b4);
//    }
//    public String read2String() throws IOException {
//        read2Len();
//        return new String(b2);
//    }
//
//    public String readStringN(int len) throws IOException {
//        return new String(readLenN(len));
//    }
//
//    private byte[] readLenN(int len) throws IOException {
//        byte[] bytes = new byte[len];
//        fileIndex += len;
//        mRandomAccessFile.read(bytes);
//        return bytes;
//    }

    private void seek(long pos) throws IOException {
        fileIndex = pos;
        mRandomAccessFile.seek(pos);
    }

    class CList extends Chunk {
        String type;
        List<Chunk> child = new ArrayList<>();

        @Override
        public String toString() {
            return "CList{" +
                    "id='" + id + '\'' +
                    ", size=" + size +
                    ", index=" + index +
                    ", type='" + type + '\'' +
                    ", child=" + child +
                    '}';
        }
    }

    class Chunk {
        String id;
        long size;
        long index;
        String describe;
        String data;

        @Override
        public String toString() {
            return "Chunk{" +
                    "id='" + id + '\'' +
                    ", size=" + size +
                    ", index=" + index +
                    ", describe='" + describe + '\'' +
                    ", data='" + data + '\'' +
                    '}';
        }
    }
}

import java.util.*;

public class LexAnalyzer {
    public static final BiDirectionalMap<String,Integer> symbolMap = new BiDirectionalMap<>(); // 关键字映射
    public static final String[] typeMap = new String[81];// 类型映射
    public StringBuffer original = new StringBuffer();// 文件传入的初始代码
    public StringBuffer str = new StringBuffer();// 经过去除注释和空格后的代码
    public List<tokenPair> tokenList = new ArrayList<>();// token表
    public int strIndex = 10000; // 字符串常量符号表地址起点
    public int digitIndex = 20000;// 数值常量符号表地址起点
    public int indentifierIndex = 30000;// 标识符符号表地址起点
    public static final int indentifierType = 80;// 标识符类型
    public static final int digitType = 79;// 数值类型
    public static final int strType = 78;// 字符串类型
    // 以下三个map用于判断是否已经读过相同标识符了，如果读过，则从map中取
    public BiDirectionalMap<String,Integer> indentifierMap = new BiDirectionalMap<>();
    public BiDirectionalMap<String,Integer> digitMap = new BiDirectionalMap<>();
    public BiDirectionalMap<String,Integer> strMap = new BiDirectionalMap<>();
    public String errorMsg = "";

    public void initMap(){
        symbolMap.put("int", 1);
        symbolMap.put("float", 2);
        symbolMap.put("char", 3);
        symbolMap.put("if", 4);
        symbolMap.put("else", 5);
        symbolMap.put("for", 6);
        symbolMap.put("while", 7);
        symbolMap.put("do", 8);
        symbolMap.put("switch", 9);
        symbolMap.put("case", 10);
        symbolMap.put("break", 11);
        symbolMap.put("long", 12);
        symbolMap.put("signed", 13);
        symbolMap.put("unsigned", 14);
        symbolMap.put("struct", 15);
        symbolMap.put("union", 16);
        symbolMap.put("enum", 17);
        symbolMap.put("typedef", 18);
        symbolMap.put("sizeof", 19);
        symbolMap.put("auto", 20);
        symbolMap.put("static", 21);
        symbolMap.put("register", 22);
        symbolMap.put("extern", 23);
        symbolMap.put("const", 24);
        symbolMap.put("volatile", 25);
        symbolMap.put("return", 26);
        symbolMap.put("continue", 27);
        symbolMap.put("goto", 28);
        symbolMap.put("default", 29);
        symbolMap.put("void", 30);
        symbolMap.put("double", 31);
        symbolMap.put("short", 32);

        symbolMap.put("+", 33);
        symbolMap.put("-", 34);
        symbolMap.put("*", 35);
        symbolMap.put("/", 36);
        symbolMap.put("=", 37);
        symbolMap.put("<", 38);
        symbolMap.put(">", 39);
        symbolMap.put("<=", 40);
        symbolMap.put(">=", 41);
        symbolMap.put("==", 42);
        symbolMap.put("!=", 43);
        symbolMap.put("|", 44);
        symbolMap.put("&", 45);
        symbolMap.put("~", 46);
        symbolMap.put("%" ,47);
        symbolMap.put("+=", 48);
        symbolMap.put("-=", 49);
        symbolMap.put("*=", 50);
        symbolMap.put("/=", 51);
        symbolMap.put("!", 52);
        symbolMap.put("||", 53);
        symbolMap.put("&&", 54);
        symbolMap.put("#", 55);

        symbolMap.put("(", 56);
        symbolMap.put(")", 57);
        symbolMap.put("[", 58);
        symbolMap.put("]", 59);
        symbolMap.put("{", 60);
        symbolMap.put("}", 61);
        symbolMap.put(";", 62);
        symbolMap.put(",", 63);
        symbolMap.put(".", 64);
        symbolMap.put(":", 65);


        for (int i = 1; i <= 32; i ++ ) typeMap[i] = "关键字";
        for (int i = 33; i <= 55; i ++ ) typeMap[i] = "运算符";
        for (int i = 56; i <= 65; i ++ ) typeMap[i] = "界符";
        typeMap[indentifierType] = "标识符";
        typeMap[digitType] = "数值常量";
        typeMap[strType] = "字符串常量";
    }
    public void readFile(String content){
        this.original.append(content);
    }
    public void delSpace(){
        StringBuffer temp = new StringBuffer();
        int j = 0, s1 = 0;
        do {
            if (original.charAt(j) == ' ' || original.charAt(j) == '\n' || original.charAt(j) == '\t') {
                while (j < original.length() && (original.charAt(j) == ' ' || original.charAt(j) == '\n' || original.charAt(j) == '\t')) {
                    j++;
                }
                temp.append(' '); // 用一个空格代替扫描到的连续空格和换行符放入str[]
                s1++;
            } else {
                temp.append(original.charAt(j));
                s1++;
                j++;
            }
        } while (j < original.length());

        if(temp.charAt(0) == ' ') temp.deleteCharAt(0);
        if(temp.charAt(temp.length()-1) == ' ') temp.deleteCharAt(temp.length()-1);
        // 末尾加个空白做类似结束符
        temp.append(' ');

        System.out.println("去除注释和空白后的结果：");
        System.out.println(temp);

        str.append(temp);
    }
    public void delComment(){
        int i, a, b, n = 0;
        do {
            if (original.charAt(n) == '/' && original.charAt(n + 1) == '*') {
                a = n; // 记录第一个注释符的位置
                while (!(original.charAt(n) == '*' && original.charAt(n + 1) == '/')) {
                    n++;
                }
                b = n + 1; // 记录第二个注释符的位置
                for (i = a; i <= b; i++) {
                    original.setCharAt(i, ' '); // 将注释的内容替换为空格
                }
            }
            n++;
        } while (n < original.length());

        n = 0;
        do {
            if (original.charAt(n) == '/' && original.charAt(n + 1) == '/') {
                a = n; // 记录第一个注释符的位置
                while (!(original.charAt(n) == '\n')) {
                    n++;
                }
                b = n;
                for (i = a; i <= b; i++) {
                    original.setCharAt(i, ' '); // 将注释的内容替换为空格
                }
            }
            n++;
        } while (n < original.length());
    }
    public boolean isDot(char ch){
        return ch == '.';
    }
    public boolean is_(char ch){
        return ch == '_';
    }
    public boolean beforeEqu(char ch){
        return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '>' || ch == '<' || ch == '='  || ch == '!';
    }
    public boolean isDigit(char ch){
        return Character.isDigit(ch);
    }
    public boolean isLetter(char ch){
        return Character.isLetter(ch);
    }
    public String bufToStr(StringBuffer s){return String.valueOf(s);}
    public String tokenToStr(List<tokenPair> tokenList){
        StringBuilder temp = new StringBuilder();
        temp.append("单词的值为-1代表种别码就是值（运算符、界符、关键字）\n");
        temp.append("单词的值为-2代表单词本身就是单词的值（数值常量）\n");
        temp.append("单词   种别码   单词的值   大类\n");
        for (tokenPair token : tokenList) {
            temp.append("<").append(token.value).append("       ").append(token.id).append("       ").append(token.innerValue).append("       ").append(token.type).append(">").append("\n");
        }
        return temp.toString();
    }
    public void storeToken(StringBuffer token, int type,int innerValue){
        if(symbolMap.containsKey(bufToStr(token))){
            // 表中有就是关键字或者符号
            int id = symbolMap.getFromKey(bufToStr(token));
            tokenList.add( new tokenPair(bufToStr(token), typeMap[id],id, -1) );
        }else {
            tokenList.add( new tokenPair(bufToStr(token), typeMap[type],type, innerValue) );
        }
    }


    public String analyze(String content) throws Exception {
        initMap();
        readFile(content);
        delComment();
        delSpace();

        int chIndex = 0, strLen = str.length();
        char ch;
        StringBuffer token = new StringBuffer();

        int currentState = 0;
        while(chIndex < strLen){
            ch = str.charAt(chIndex);

            switch (currentState) {
                case 0:
                    token.delete(0,token.length());

                    if(isLetter(ch)){
                        currentState = 8;
                        token.append(ch);
                    }else if(isDigit(ch)){
                        currentState = 1;
                        token.append(ch);
                    }else if(beforeEqu(ch)){
                        currentState = 9;
                        token.append(ch);
                    }else if(ch == '|'){
                        currentState = 14;
                        token.append(ch);
                    }else if(ch == '&'){
                        currentState = 16;
                        token.append(ch);
                    }else if(symbolMap.containsKey(String.valueOf(ch))){
                        currentState = 11;
                        token.append(ch);
                    }else if(ch == '"'){
                        currentState = 12;
                        token.append(ch);
                    }else if(ch == ' ' || ch == '\t' || ch == '\n'){
                        currentState = 0;
                    }else {
                        errorMsg = "error0";
                        return tokenToStr(tokenList);
                    }
                    break;
                case 1:
                    if(isDigit(ch)){
                        currentState = 1;
                        token.append(ch);
                    }else if (isDot(ch)){
                        currentState = 2;
                        token.append(ch);
                    }else if (ch == 'e' || ch == 'E'){
                        currentState = 4;
                        token.append(ch);
                    }else{
//                        if(!digitMap.containsKey(token.toString())){
//                            digitIndex++;
//                            digitMap.put(token.toString(),digitIndex);
//                        }
                        storeToken(token,digitType, -2);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 2:
                    if(isDigit(ch)){
                        currentState = 3;
                        token.append(ch);
                    }else {
                        errorMsg = "error2: missing number after dot";
                        return tokenToStr(tokenList);
                    }
                    break;
                case 3:
                    if(isDigit(ch)){
                        currentState = 3;
                        token.append(ch);
                    }else if(ch == 'e' || ch == 'E'){
                        currentState = 4;
                        token.append(ch);
                    }else {
//                        if(!digitMap.containsKey(token.toString())){
//                            digitIndex++;
//                            digitMap.put(token.toString(),digitIndex);
//                        }
                        storeToken(token,digitType, -2);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 4:
                    if(isDigit(ch)){
                        currentState = 7;
                        token.append(ch);
                    }else if(ch == '+' || ch == '-'){
                        currentState = 5;
                        token.append(ch);
                    }else {
                        errorMsg = "error4: missing object after e/E";
                        return tokenToStr(tokenList);
                    }
                    break;
                case 5:
                    if(isDigit(ch)){
                        currentState = 6;
                        token.append(ch);
                    }else {
                        errorMsg = "error: missing object after e/E+";
                        return tokenToStr(tokenList);
                    }
                    break;
                case 6:
                    if(isDigit(ch)){
                        currentState = 6;
                        token.append(ch);
                    }else {
//                        if(!digitMap.containsKey(token.toString())){
//                            digitIndex++;
//                            digitMap.put(token.toString(),digitIndex);
//                        }
                        storeToken(token,digitType, -2);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 7:
                    if(isDigit(ch)){
                        currentState = 7;
                        token.append(ch);
                    }else {
                        if(!digitMap.containsKey(token.toString())){
                            digitIndex++;
                            digitMap.put(token.toString(),digitIndex);
                        }
                        storeToken(token,digitType, -2);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 8:
                    if(is_(ch) || isDigit(ch) || isLetter(ch)){
                        currentState = 8;
                        token.append(ch);
                    }else {
                        int innerVal = -1;
                        if(symbolMap.containsKey(token.toString())){
                            innerVal = -1;
                        }else if(!indentifierMap.containsKey(token.toString())){
                            indentifierIndex++;
                            indentifierMap.put(token.toString(),indentifierIndex);
                            innerVal = indentifierIndex;
                        }else{
                            innerVal = indentifierIndex;
                        }
                        storeToken(token,indentifierType, innerVal);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 9:
                    if (ch == '='){
                        currentState = 10;
                        token.append(ch);
                    }else {
                        storeToken(token,-1,-1);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 10:
                case 11:
                case 15:
                case 17:
                    storeToken(token,-1,-1);
                    chIndex--;
                    currentState = 0;
                    break;
                case 12:
                    if(ch == '"') {
                        currentState = 13;
                        token.append(ch);
                    }else if (chIndex+1 >= strLen) {
                        errorMsg = "error: no \" matches \"";
                        return tokenToStr(tokenList);
                    }else {
                        currentState = 12;
                        token.append(ch);
                    }
                    break;
                case 13:
                    if(!strMap.containsKey(token.toString())){
                        strIndex++;
                        strMap.put(token.toString(),strIndex);
                    }
                    storeToken(token,strType, strMap.getFromKey(token.toString()));
                    chIndex--;
                    currentState = 0;
                    break;
                case 14:
                    if (ch == '|'){
                        currentState = 15;
                        token.append(ch);
                    }else {
                        storeToken(token,-1,-1);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                case 16:
                    if (ch == '&'){
                        currentState = 17;
                        token.append(ch);
                    }else {
                        storeToken(token,-1,-1);
                        chIndex--;
                        currentState = 0;
                    }
                    break;
                default:
                    errorMsg = "\nerror default";
                    return tokenToStr(tokenList);
            }
            chIndex++;
        }

        // 语法分析
        Parser parser = new Parser(tokenList);
        parser.analyse();

        return tokenToStr(tokenList);
    }
}



class tokenPair {
    public String value;// 字符串
    public int id;// 种别码
    public int innerValue;// 值
    public String type;// 类型

    public tokenPair(String value, String type, int id, int innerValue) {
        this.value = value;
        this.id = id;
        this.innerValue = innerValue;
        this.type = type;
    }
}
class BiDirectionalMap<K, V> {
    public Map<K, V> map1 = new HashMap<>();
    public Map<V, K> map2 = new HashMap<>();

    // 添加键值对 (a, b)
    public void put(K a, V b) {
        map1.put(a, b);
        map2.put(b, a);
    }

    // 根据键 a 获取值 b
    public V getFromKey(K a) {
        return map1.get(a);
    }

    // 根据键 b 获取值 a
    public K getFromValue(V b) {
        return map2.get(b);
    }

    // 判断是否包含键 a
    public boolean containsKey(K a) {
        return map1.containsKey(a);
    }

    // 判断是否包含键 b
    public boolean containsValue(V b) {
        return map2.containsKey(b);
    }

    // 获取当前映射的大小
    public int size() {
        return map1.size();
    }
}
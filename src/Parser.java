import java.util.*;
import java.io.*;
import java.util.function.Function;

public class Parser {
    public List<tokenPair> tokenList;
    public List<String> tokens;
    // String是某个终结符或者非终结符，Set<String>是它的First集合
    public Map<String, Set<String>> FirstVt;
    // String是产生式的左侧，List<String>是产生式右侧集合
    public List<Map<String, List<String>>> productions;
    // 终结符集合
    public Set<String> terminators;
    // 非终结符集合
    public Set<String> NotTerminators;
    // 产生式集合，整理后以产生式左侧String为唯一键值，给产生式右侧加了产生式编号Integer
    public Map<String, List<Pair<List<String>, Integer>>> productionsVT;
    // LR(1)分析表, Interger是状态，
    public Map<Integer, Map<String, Pair<String, Integer>>> LRTable;

    private static final String filePath = "./GRAtest1.txt";
    private static final String saveAs = "Action_and_Goto3.doc";
    private static final String startL = "S";
    private static final String startR = "G";

    public Parser(List<tokenPair> tokenList) {
        this.tokenList = new ArrayList<>(tokenList);
        this.tokens = new ArrayList<>();
        this.FirstVt = new HashMap<>();
        this.productions = new ArrayList<>();
        this.terminators = new HashSet<>();
        this.NotTerminators = new HashSet<>();
        this.productionsVT = new HashMap<>();
        this.LRTable = new HashMap<>();
    }

    // 读取文法
    public void ReadSynGrammar(String filename) throws Exception {
        Scanner scanner = new Scanner(new File(filename));
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split(":");
            String Curleft = parts[0];
            String Curright = parts[1];
            List<String> Rlist = new ArrayList<>();
            if (Curright.contains(" ")) {
                Rlist = Arrays.asList(Curright.split(" "));
            } else {
                Rlist.add(Curright);
            }
            Map<String, List<String>> production = new HashMap<>();
            production.put(Curleft, Rlist);
            this.productions.add(production);
        }
        scanner.close();
    }

    // 初始化终结符和非终结符
    public void initTerminatorsAndNot() {
        Set<String> termSet = new HashSet<>();
        for (Map<String, List<String>> pro : this.productions) {
            for (String left : pro.keySet()) {
                if (!this.productionsVT.containsKey(left)) {
                    this.productionsVT.put(left, new ArrayList<>());
                }
                this.productionsVT.get(left).add(new Pair<>(pro.get(left), this.productions.indexOf(pro)));
                termSet.add(left);
                this.NotTerminators.add(left);
                termSet.addAll(pro.get(left));
            }
        }
        this.terminators = termSet;
        // 终结符集合 = 全部元素 - 非终结符集合
        this.terminators.removeAll(this.NotTerminators);
    }

    // 获取cur的整个First集
    public Set<String> getFirstVt(String Cur, Set<String> isfangwen) {
        if (this.FirstVt.containsKey(Cur)) {
            return this.FirstVt.get(Cur);
        }
        Set<String> tempset = new HashSet<>();
        if (this.terminators.contains(Cur)) {
            tempset.add(Cur);
            return tempset;
        }
        isfangwen.add(Cur);
        for (Pair<List<String>, Integer> pair : this.productionsVT.get(Cur)) {
            boolean flag = true;
            for (String right : pair.getKey()) {
                if (right.equals("$")) {
                    tempset.add("$");
                    break;
                }
                if (isfangwen.contains(right)) {
                    continue;
                }
                Set<String> cur_set = this.getFirstVt(right, isfangwen);
                if (cur_set.contains("$")) {
                    tempset.addAll(cur_set);
                    tempset.remove("$");
                } else {
                    tempset.addAll(cur_set);
                    flag = false;
                    break;
                }
            }
            if (flag) {
                tempset.add("$");
            }
        }
        return tempset;
    }

    // 获取所有元素的First集
    public void GetFirstVt() {
        for (String terminator : this.terminators) {
            this.FirstVt.put(terminator, this.getFirstVt(terminator, new HashSet<>()));
        }
        for (String NotTerminator : this.NotTerminators) {
            this.FirstVt.put(NotTerminator, this.getFirstVt(NotTerminator, new HashSet<>()));
        }
    }

    // 获取产生式的闭包
    public Set<ClosureItem> getCLOSURE(int productionID, String nonTerminal, List<String> productionList, int point, Set<String> searchSet, Set<ClosureItem> CLOitem) {
        ClosureItem curItem = new ClosureItem(productionID, nonTerminal, productionList, point, searchSet);

        CLOitem.add(curItem);

        if (point < productionList.size() && NotTerminators.contains(productionList.get(point))) {
            // 先计算以此非终结符为左侧产生式的向前搜索符集合
            Set<String> tempSet = new HashSet<>();
            boolean flag = true;
            for (int i = point + 1; i < productionList.size(); i++) {
                Set<String> curFirst = new HashSet<>(FirstVt.get(productionList.get(i)));
                if (curFirst.contains("$")) {
                    // 如果first集存在空，则并上first(Yi)-{$}，并且继续循环
                    curFirst.remove("$");
                    tempSet.addAll(curFirst);
                } else {
                    // 如果first集没有空，则并上当前first集，并且结束循环
                    flag = false;
                    tempSet.addAll(curFirst);
                    break;
                }
            }
            if (flag) {
                // 如果右边的产生式first集全有空，则还要并上左边的向前搜索符集
                tempSet.addAll(searchSet);
            }
            // 对点以后的非终结符作为左侧时的产生式逐个进行遍历
            List<Pair<List<String>, Integer>> proList = productionsVT.get(productionList.get(point));
            for (Pair<List<String>, Integer> pro : proList) {
                ClosureItem tempItem = new ClosureItem();
                tempItem.productionID = pro.getValue();
                tempItem.nonTerminal = productionList.get(point);
                tempItem.productionList = pro.getKey();
                tempItem.point = 0;
                tempItem.searchSet = Collections.unmodifiableSet(tempSet);

                // 存疑
                if (!hasItem(CLOitem,tempItem)) {
                    CLOitem.addAll(getCLOSURE(pro.getValue(), productionList.get(point), pro.getKey(), 0, tempSet, CLOitem));
                }
            }

            // 合并项目集中的相同的项目
            Map<List<Object>, Set<String>> tempItemDict = new HashMap<>();
            for (ClosureItem item : CLOitem) {
                List<Object> keyList = Arrays.asList(item.productionID, item.nonTerminal, item.productionList, item.point);
                // CONTAINSKEY存疑
                if (hasKey(tempItemDict,keyList).getKey()) {
                    tempItemDict.remove(hasKey(tempItemDict,keyList).getValue());
                }
                tempItemDict.put(keyList, new HashSet<>());
                tempItemDict.get(keyList).addAll(item.searchSet);
            }
            CLOitem.clear();
            for (Map.Entry<List<Object>, Set<String>> entry : tempItemDict.entrySet()) {
                List<Object> keyList = entry.getKey();
                ClosureItem ansItem = new ClosureItem((int) keyList.get(0), (String) keyList.get(1), (List<String>) keyList.get(2), (int) keyList.get(3), entry.getValue());
                CLOitem.add(ansItem);
            }
        }
        return CLOitem;
    }


    // 创建LR(1)分析表
    public void createLRTable() {
        // Status有编号和LRDFANode(编号和项目集)
        Map<Integer, LRDFANode> Status = new HashMap<>();
        // SCLOitem有编号和项目集，类似LRDFANode
        Map<Set<ClosureItem>, Integer> SCLOitem = new HashMap<>();

        // helper function to get LRDFANode object with the given ID
        Function<Integer, LRDFANode> getLRDFANode = (id) -> {
            if (Status.containsKey(id)) {
                return Status.get(id);
            } else {
                LRDFANode newNode = new LRDFANode(id);
                Status.put(id, newNode);
                return newNode;
            }
        };
        // build initial item set I0
        int startId = 0;
        LRDFANode startNode = getLRDFANode.apply(startId);
        // calculate closure of the initial itemE
        List<String> prolist = new ArrayList<>();
        prolist.add(startR);
        Set<String> searchset = new HashSet<>();
        searchset.add("@");
        Set<ClosureItem> startClosure = getCLOSURE(0, startL, prolist, 0, searchset, new HashSet<ClosureItem>());
        // add closure to the initial item set
        startNode.addItemsets(startClosure);
        SCLOitem.put(startClosure, startId);
        Status.put(startId, startNode);

        // use BFS to build the collection of item sets
        Queue<LRDFANode> queue = new LinkedList<LRDFANode>();
        queue.add(startNode);
        int mid = 0;
        while (!queue.isEmpty()) {
            LRDFANode nowNode = queue.remove();
            Set<ClosureItem> nowItemsets = nowNode.itemsets;
            int nowId = nowNode.id;
            Set<ClosureItem> isVisit = new HashSet<>();

            // 对项目集中的每个产生式
            for (ClosureItem item : nowItemsets) {
                // 存疑
                if (hasItem(isVisit,item)) {
                    continue;
                }
                isVisit.add(item);
                int index = item.productionID;
                String left = item.nonTerminal;
                List<String> rList = item.productionList;
                int point = item.point;
                Set<String> search = item.searchSet;

                // 当点在最后或者产生式的右边有空时进行归约操作
                if (point >= rList.size() || rList.contains("$")) {
                    if (!LRTable.containsKey(nowId)) {
                        LRTable.put(nowId, new HashMap<>());
                    }
                    for (String s : search) {
                        // 归约到不同的产生式
                        if (LRTable.get(nowId).containsKey(s)) {
                            // 规约-规约冲突
                            System.out.println("当前文法不属于LR(1)文法！！");
                            System.out.println("发生了规约-规约冲突！");
                            return;
                        }
                        // 形成LR(1)分析表中的归约式
                        LRTable.get(nowId).put(s, new Pair<>("r", index));
                    }
                } else {
                    // 若点在中间，则移动点获得新的项目集
                    // 点向后移一位得到一个新项目集
                    // 求新项目的closure闭包
                    Set<ClosureItem> nextClosure = getCLOSURE(index, left, rList, point + 1, search, new HashSet<>());
                    String nextCin = rList.get(point);

                    // 遍历上一个项目集中所有可以输入nextCin的产生式 对其进行 闭包 并入
                    for (ClosureItem i : nowItemsets) {
                        // 存疑
                        if (hasItem(isVisit,i)) {
                            continue;
                        }
                        List<String> iRight = i.productionList;
                        int iPoint = i.point;
                        // 若点在中间并且点右边的字符正好为 nextCin
                        if (iPoint < iRight.size() && iRight.get(iPoint).equals(nextCin)) {
                            isVisit.add(i);
                            nextClosure.addAll(getCLOSURE(i.productionID, i.nonTerminal, i.productionList, iPoint + 1, i.searchSet, new HashSet<>()));
                        }
                    }

                    // 合并新项目集中的相同的项目
                    // 先将不同的元组的0123(元组形式)作为新字典的key 4作为新字典的value存入字典中去除相同项目再转化为元组存储到到集合中
                    Map<List<Object>, Set<String>> newItemSet = new HashMap<>();
                    for (ClosureItem item2 : nextClosure) {
                        List<Object> itemKey = Arrays.asList(item2.productionID, item2.nonTerminal, item2.productionList, item2.point);
                        // // CONTAINSKEY存疑
                        if (hasKey(newItemSet,itemKey).getKey()) {
                            newItemSet.remove(hasKey(newItemSet,itemKey).getValue());
                        }
                        newItemSet.put(itemKey, new HashSet<>());
                        newItemSet.get(itemKey).addAll(item2.searchSet);
                    }
                    Set<ClosureItem> nextItemSet = new HashSet<>();
                    for (Map.Entry<List<Object>, Set<String>> entry : newItemSet.entrySet()) {
                        List<Object> keyList = entry.getKey();
                        ClosureItem finalItem = new ClosureItem((int) keyList.get(0), (String) keyList.get(1), (List<String>) keyList.get(2), (int) keyList.get(3), entry.getValue());
                        nextItemSet.add(finalItem);
                    }

                    int nextNodeId;
                    if ( this.hasKey(SCLOitem,nextItemSet).getKey() ) { // 该项目集已经处理过
                        nextNodeId = this.hasKey(SCLOitem,nextItemSet).getValue();
                    } else { // 新建一个项目集
                        mid++;
                        nextNodeId = mid;
                        SCLOitem.put(nextItemSet, nextNodeId);
                        LRDFANode nextNode = getLRDFANode.apply(nextNodeId);
                        nextNode.addItemsets(nextItemSet);
                        Status.put(nextNodeId, nextNode);
                        queue.add(nextNode);
                    }
                    if (!LRTable.containsKey(nowId)) { // 为当前项目集在 LR 分析表中创立表项
                        LRTable.put(nowId, new HashMap<>());
                    }
                    // 移进-规约冲突
                    if (LRTable.get(nowId).containsKey(rList.get(point))) {
                        System.out.println("当前文法不属于 LR(1) 文法！！！");
                        System.out.println("发生了移进-规约冲突！");
                        return;
                    }
                    if (terminators.contains(rList.get(point))) {
                        LRTable.get(nowId).put(rList.get(point), new Pair<>("S", nextNodeId));
                    } else {
                        LRTable.get(nowId).put(rList.get(point), new Pair<>("G", nextNodeId));
                    }
                }
            }
        }

        // 创建输出文件
        File outputFile = new File(saveAs);
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 写入数据
        try {
            FileWriter writer = new FileWriter(outputFile);
            writer.write("[d1,s1]=s2d2 代表当处于状态d1，遇到s1时，会执行s2d2操作\n");
            writer.write("r代表规约，G代表GOTO,S代表转移到,d2代表第d2条产生式\n");
            writer.write("其中@代表书上的$符(我们用$代表产生式中的ε)\n\n");
            for (int id : LRTable.keySet()) {
                for (String i : LRTable.get(id).keySet()) {
                    String ans = LRTable.get(id).get(i).getKey() + LRTable.get(id).get(i).getValue();
                    writer.write(String.format("[%d,%s]=%s\t", id, i, ans));
                }
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean run(PrintStream out) {
        Stack<Integer> statusStack = new Stack<>();
        statusStack.add(0);
        Stack<String> symbolStack = new Stack<>();
        symbolStack.add("@");
        tokens.add("@");
        Collections.reverse(tokens);
        boolean isacc = false;
        int count = 0;
        while (true) {
            count++;
            int topStatus = statusStack.peek();
            String token = tokens.get(tokens.size() - 1);
            out.println("步骤: " + count);
            out.println("输入符号: " + token);
            out.println("符号栈:");
            out.println(symbolStack);
            out.println("状态栈:");
            out.println(statusStack);
            out.println();
            if (this.LRTable.get(topStatus).containsKey(token)) {
                Pair<String,Integer> act = this.LRTable.get(topStatus).get(token);
                if (act.getKey().equals("S")) {
                    statusStack.push(act.getValue());
                    symbolStack.push(token);
                    tokens.remove(tokens.size() - 1);
                } else if (act.getKey().equals("r")) {
                    if (act.getValue() == 0) {
                        isacc = true;
                        break;
                    }
                    Map<String, List<String>> production = this.productions.get(act.getValue());
                    String left = (String) production.keySet().toArray()[0];
                    // 下一轮自然就是分析left，就会到GOTO状态
                    tokens.add(left);
                    // 产生式为空A->ε不需要出栈
                    if (production.get(left).equals(List.of("$"))) {
                        continue;
                    }
                    // 弹出产生式右侧相等数量的状态
                    int rLength = production.get(left).size();
                    for (int i = 0; i < rLength; i++) {
                        statusStack.pop();
                        symbolStack.pop();
                    }
                } else {
                    // 遇到非终结符GOTO
                    statusStack.push(act.getValue());
                    symbolStack.push(token);
                    tokens.remove(tokens.size() - 1);
                }
            } else {
                out.println("发现: " + token);
                out.println("期待字符为:");
                for (String exp : this.LRTable.get(topStatus).keySet()) {
                    out.println(exp);
                }
                break;
            }
        }
        return isacc;
    }



    public void analyse() throws Exception {
        preTokenList();
        ReadSynGrammar(filePath);
        initTerminatorsAndNot();
        GetFirstVt();
        createLRTable();
        try (PrintStream output = new PrintStream(new FileOutputStream("out.doc"))) {
            if(run(output)){
                output.println("该代码符合语法");
            }else {
                output.println("该代码不符合语法");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void preTokenList(){
        for(tokenPair t : tokenList){
            if(t.innerValue == -1){
                tokens.add(t.value);
            }else if(t.innerValue == -2){
                tokens.add("DIGIT");
            }else if(t.innerValue>=30000){
                tokens.add("IDENTIFIER");
            }else if(t.innerValue>=20000){
                tokens.add("STRING");
            }
        }
    }

    public boolean hasItem(Set<ClosureItem> s,ClosureItem c2){
        for (ClosureItem c1 : s) {
            if (c1.productionID == c2.productionID && c1.point == c2.point && c1.searchSet.size() == c2.searchSet.size() && c1.searchSet.containsAll(c2.searchSet)) {
                return true;
            }
        }
        return false;
    }

    public Pair<Boolean,Integer> hasKey(Map<Set<ClosureItem>,Integer> SCLOitem, Set<ClosureItem> itemSet){
        if(SCLOitem.containsKey(itemSet)){
            return new Pair<>(true,SCLOitem.get(itemSet));
        }
        for (Map.Entry<Set<ClosureItem>,Integer> entry : SCLOitem.entrySet()) {
            Set<ClosureItem> s1 = entry.getKey();
            if (cloEquals(s1,itemSet)) {
                return new Pair<>(true,SCLOitem.get(s1));
            }
        }
        return new Pair<>(false,-1);
    }
    public boolean cloEquals(Set<ClosureItem> s1, Set<ClosureItem> s2){
        if(s1.size() != s2.size()){
            return false;
        }
        for (ClosureItem c1 : s1) {
            if(!hasItem(s2,c1)){
                return false;
            }
        }
        return true;
    }

    public Pair<Boolean,List<Object>> hasKey(Map<List<Object>, Set<String>> SCLOitem, List<Object> itemSet){
        if(SCLOitem.containsKey(itemSet)){
            return new Pair<>(true,itemSet);
        }
        for (Map.Entry<List<Object>, Set<String>> entry : SCLOitem.entrySet()) {
            List<Object> keySet = entry.getKey();
            if((int)keySet.get(0) == (int)itemSet.get(0) && (int)keySet.get(3) == (int)itemSet.get(3)){
                return new Pair<>(true,keySet);
            }
        }
        return new Pair<>(false,null);
    }

}

class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public void setValue(V value) {
        this.value = value;
    }
}

class LRDFANode {
    int id;
    Set<ClosureItem> itemsets;

    public LRDFANode(int id) {
        this.id = id;
        this.itemsets = new HashSet<>();
    }

    public void addItemsets(Set<ClosureItem> itemset) {
        this.itemsets.addAll(itemset);
    }
}

class ClosureItem {
    public int productionID;
    public String nonTerminal;
    public List<String> productionList;
    public int point;
    public Set<String> searchSet;

    public ClosureItem(int productionID, String nonTerminal, List<String> productionList, int point, Set<String> searchSet) {
        this.productionID = productionID;
        this.nonTerminal = nonTerminal;
        this.productionList = productionList;
        this.point = point;
        this.searchSet =searchSet;
    }
    public ClosureItem(){}
}
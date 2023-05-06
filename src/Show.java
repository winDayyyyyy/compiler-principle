import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;

public class Show {
    String content;//存放需要解析的串
    String res;//存放解析之后的串
    String error;//存放错误信息的串
    int flag = 1;
    private static void InitGlobalFont(Font font) {
        FontUIResource fontRes = new FontUIResource(font);
        for (Enumeration<Object> keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontRes);
            }
        }
    }
    public void initUI(){
        InitGlobalFont(new Font("微软雅黑", Font.PLAIN, 16));
        JFrame hi;
        hi = new JFrame();
        hi.setSize(300, 300);
        hi.setTitle("实验1 词法分析器");
        hi.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        hi.setLocationRelativeTo (null);
        hi.setLayout(null);
        JButton item_add = new JButton("开始实验");
        item_add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                hi.setVisible(false);
                game_view();
            }
        });
        item_add.setBounds(90,100,100,50);
        hi.add(item_add);
        hi.setVisible(true);
    }
    public void game_view(){
        InitGlobalFont(new Font("微软雅黑", Font.PLAIN, 16));
        JFrame wow;
        wow = new JFrame();
        wow.setSize(1300, 800);
        wow.setTitle("实验1 词法分析器");
        wow.setLocationRelativeTo (null);
        wow.setLayout(null);
        wow.setVisible(true);

        JButton select_button,start_button,clear_button,quit_button;
        select_button = new JButton("选择文件");
        start_button = new JButton("开始解析");
        clear_button = new JButton("清空");
        quit_button = new JButton("结束实验");
        select_button.setBounds(50,30,100,30);
        start_button.setBounds(170,30,100,30);
        clear_button.setBounds(290,30,100,30);
        quit_button.setBounds(1100,30,100,30);
        wow.add(select_button);
        wow.add(start_button);
        wow.add(clear_button);
        wow.add(quit_button);


        JLabel label1 = new JLabel("待解析的串");
        JLabel label2 = new JLabel("解析后的串");
        JLabel label3 = new JLabel("错误信息");
        JLabel label4 = new JLabel("实验一 词法分析器");
        label1.setBounds(180,60,150,50);
        label2.setBounds(600,60,150,50);
        label3.setBounds(1020,60,150,50);
        label4.setBounds(700,30,150,30);
        wow.add(label1);
        wow.add(label2);
        wow.add(label3);
        wow.add(label4);


        JScrollPane bscrollpane1 = new JScrollPane();
        bscrollpane1.setBounds(50,100,400,650);
        JScrollPane bscrollpane2 = new JScrollPane();
        bscrollpane2.setBounds(450,100,400,650);
        JScrollPane bscrollpane3 = new JScrollPane();
        bscrollpane3.setBounds(850,100,400,650);
        wow.add(bscrollpane1);
        wow.add(bscrollpane2);
        wow.add(bscrollpane3);

//按钮监测的事件
        //选择文件按钮，最终将选择文件的内容放到content变量中
        select_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    String filePath = selectedFile.getAbsolutePath();
                    // 在这里使用所选文件的路径
                    try {
                        FileReader fileReader = new FileReader(filePath);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                        String line;
                        JTextArea textArea = new JTextArea();
                        while ((line = bufferedReader.readLine()) != null) {
                            textArea.append(line + "\n");
                        }
                        bufferedReader.close();
                        bscrollpane1.setViewportView(textArea);
                        content = textArea.getText();
                    }catch (IOException m) {
                        m.printStackTrace();
                    }
                }
            }
        });

        //开始解析按钮，调用package1包中slove类中的解析函数slove_str()，传入content作为参数，返回res作为结果
        start_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LexAnalyzer obj = new LexAnalyzer();

                // 词法语法分析入口
                try {
                    res = obj.analyze(content);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

                JTextArea textArea1 = new JTextArea();
                JTextArea textArea2 = new JTextArea();
                //flag可改动，当无错误信息时，flag置为1
                if(!obj.errorMsg.isEmpty()){
                    textArea1.append(res);
                    bscrollpane2.setViewportView(textArea1);
                    textArea2.append(obj.errorMsg);
                    bscrollpane3.setViewportView(textArea2);
                }
                else {
                    textArea1.append(res);
                    bscrollpane2.setViewportView(textArea1);
                    textArea2.append("无");
                    bscrollpane3.setViewportView(textArea2);
                }
            }
        });

        clear_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JViewport viewport1 =  bscrollpane1.getViewport();
                // 获取JTextArea组件
                JTextArea textArea1 = (JTextArea) viewport1.getView();
                // 清空文本
                textArea1.setText("");

                JViewport viewport2 =  bscrollpane2.getViewport();
                JTextArea textArea2 = (JTextArea) viewport2.getView();
                textArea2.setText("");

                JViewport viewport3 =  bscrollpane3.getViewport();
                JTextArea textArea3 = (JTextArea) viewport3.getView();
                textArea3.setText("");
            }
        });

        quit_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wow.setVisible(false);
                end();
            }
        });
    }

    public void end(){
        InitGlobalFont(new Font("微软雅黑", Font.PLAIN, 16));
        JOptionPane.showMessageDialog(null,
                "谢谢参与",
                "提示",JOptionPane.WARNING_MESSAGE);
    }
    public static void main(String[] args){
        Show display = new Show();
        display.initUI();
    }
}

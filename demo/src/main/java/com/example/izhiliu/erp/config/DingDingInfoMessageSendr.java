package com.izhiliu.erp.config;

import com.izhiliu.dingtalk.notice.mobile.PhoneNumberEnumerate;
import com.izhiliu.dingtalk.notice.sender.TextMessageSender;
import org.springframework.core.env.Environment;

import java.util.HashMap;


public class DingDingInfoMessageSendr  extends TextMessageSender{

    Environment env;
    public DingDingInfoMessageSendr(Environment env) {
       this.env = env;
    }


    HashMap<String,Integer> stringIntegerHashMap =new HashMap<>();
    /**
     *
     * @param content
     * @param notifieds   通知给谁
     * @param isAtAll   在钉钉里面@谁
     * @param e
     */
    public void send(StringBuilder content, String[]  notifieds, boolean isAtAll,Throwable e) {
        if(!isSand(content)){
            return;
        }
        content.append("】exception【").append(e instanceof NullPointerException?" null ":e.getMessage());
        content.append("\n");
        int index  = 0;
        for (StackTraceElement stackTraceElement : e.getStackTrace()) {
            if(index > 20){
                break;
            }
            ++index;
           content.append(stackTraceElement.getClassName()).append(".").append(stackTraceElement.getMethodName()).append("(").append(stackTraceElement.getFileName()).append(":").append(stackTraceElement.getLineNumber()).append(")\n");
        }
        send(content, notifieds, isAtAll);
    }
    public void send(StringBuilder content, String[] notifieds, boolean isAtAll) {
        if(!isSand(content)){
            return;
        }
        content.insert(0,"】  ").insert(0, env.getProperty("spring.profiles.active")).insert(0,"】 【").insert(0,env.getProperty("spring.application.name")).insert(0,"【");
        super.send(content.toString(), notifieds, isAtAll);
    }

    public void send(StringBuilder content) {
        send(content.toString(), new String[]{PhoneNumberEnumerate.Levi.getMobile()}, true);
    }

    public void send(StringBuilder content,Throwable e) {
        send(content, new String[]{PhoneNumberEnumerate.Levi.getMobile()}, true,e);
    }


    /**
     *  防止爆炸设定
     */
    public boolean  isSand(StringBuilder  content){
        final String s = content.toString();
        boolean isSand =  false;
        Integer integer = stringIntegerHashMap.getOrDefault(s,-1);
        if(integer == -1){
            stringIntegerHashMap.put(s,1);
            isSand = true;
        }
        if((integer % 100 ) == 0 ) {
            content.append(" 【 当前爆炸指数").append(integer).append("  】");
            isSand = true;

        }
        stringIntegerHashMap.put(s,++integer);
        return  isSand;
    }
}

package com.izhiliu.erp.util;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


public class PdfManager {



    /**
     * 第二种解决方案，统一按照宽度压缩 这样来的效果是，所有图片的宽度是相等的，自我认为给客户的效果是最好的
     */
    public static int getPercent2(float h, float w) {
        int p = 0;
        float p2 = 0.0f;
        p2 = 530 / w * 100;
        p = Math.round(p2);
        return p;
    }

    /**
     * 第一种解决方案 在不改变图片形状的同时，判断，如果h>w，则按h压缩，否则在w>h或w=h的情况下，按宽度压缩
     *
     * @param h
     * @param w
     * @return
     */

    public static int getPercent(float h, float w) {
        int p = 0;
        float p2 = 0.0f;
        if (h > w) {
            p2 = 297 / h * 100;
        } else {
            p2 = 210 / w * 100;
        }
        p = Math.round(p2);
        return p;
    }

    /**
     * 遍历图片文件
     */
    public static ArrayList<String> showDirectory(File file) {
        ArrayList<String> imageUrllist = new ArrayList<>();
        File[] files = file.listFiles();
        for (File f : files) {
            //如果是文件夹继续遍历
            if (f.isDirectory()) {
                showDirectory(f);
            }
            imageUrllist.add(f.getAbsolutePath());
        }
        return imageUrllist;
    }


}

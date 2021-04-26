package com.sixlengs.path2mysql.util;

import java.util.ArrayList;
import java.util.List;

/**
 * description:
 *
 * @author wubiao
 * @version 1.0
 * @date 2020/6/27
 */
public class ListSplitUtils {



    public static <T> List<List<T>> subListByNum(List<T> data, int step) {
        // 商和余数
        int zu = data.size() / step;
        int yu = data.size() % step;
        // 切割点
        List<Integer> indexArray = new ArrayList<>();
        for (int i = 0, index = 0; i <= zu; i++, index += step) {
            indexArray.add(index);
        }
        // 如果不是整除,添上余数
        if (yu != 0) {
            indexArray.add(indexArray.get(indexArray.size() - 1) + yu);
        }
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < indexArray.size() - 1; i++) {
            List<T> subList = data.subList(indexArray.get(i), indexArray.get(i + 1));
            result.add(subList);
        }
        return result;
    }
}
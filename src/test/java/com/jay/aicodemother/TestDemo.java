/**
 * Class name: TestDemo
 * Package: com.jay.aicodemother
 * Description:
 *
 * @Create: 2025/10/16 16:05
 * @Author: jay
 * @Version: 1.0
 */
package com.jay.aicodemother;

import java.util.ArrayList;
import java.util.List;

public class TestDemo {

    public List<List<String>> partition(String s){
        List<List<String>> ans = new ArrayList<>();
        List<String> path = new ArrayList<>();
        dfs(s, 0, path, ans);
        return ans;
    }

    private void dfs(String s, int index, List<String> path, List<List<String>> ans){
        if(index == s.length()){
            ans.add(new ArrayList<>(path));
            return;
        }
        for(int i = index; i < s.length(); i++){
            if(isPalindrome(s, index, i)){
                path.add(s.substring(index, i + 1));
                dfs(s, i + 1, path, ans);
                path.remove(path.size() - 1);
            }
        }
    }

    private boolean isPalindrome(String s, int index, int i) {
        while(index < i){
            if(s.charAt(index) != s.charAt(i)){
                return false;
            }
            index++;
            i--;
        }
        return true;
    }

    public static void main(String[] args) {
        System.out.println(new TestDemo().partition("aab"));
    }
}
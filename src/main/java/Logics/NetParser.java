package Logics;

import Tool.FileProcesser;
import Tool.Solution;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NetParser {

    /**
     * 范围集
     */
    private List<Solution> solutions = new ArrayList<>();

    /**
     * 解析神经网络参数，获得各种情况的对应范围集
     * @param weights 神经网络权重参数
     * @return 范围集
     */
    public List<Solution> parse(List<double[][]> weights){
        solutions.clear();
        int input_number = calculateInputNumber(weights);

        double[][] input_layer = new double[input_number + 1][input_number + 1];
        for (int i = 0; i <= input_number; ++i)
            input_layer[i][i] = 1;

        calculateNet(input_layer, weights, 0, weights.size(), new ArrayList<>());

        return solutions;
    }

    /**
     * 动态规划计算网络逐层节点的线性表达式
     * @param input_layer 上一层所有节点的线性表达式
     * @param weights 节点权重
     * @param layer_index 指明当前的层数
     * @param layer_nubmer 指明最大的层数
     * @param condition 已经记录的约束的线性不等式方程组
     */
    private void calculateNet(double[][] input_layer, List<double[][]> weights, int layer_index, int layer_nubmer, List<double[]> condition){
        //如果已经计算到输出层，则将结果记录后返回
        if(layer_index == layer_nubmer) {
            Solution solution = new Solution(input_layer, condition);
            solutions.add(solution);
            FileProcesser.recordSolution(solution, solutions.size());
            return;
        }

        //获取当前层的所有权重
        double[][] weight = weights.get(layer_index);
        //获取当前一层节点线性表达式全部大于0的计算结果
        double[][][][] cache = attainNodeCache(input_layer, weight);

        if(layer_index++ == 0){
            //当该层为输入层时
            //递归调用实现动态规划
            calculateNet(calculateLayer(cache,0), weights, layer_index, layer_nubmer, condition);
        }else {
            //当该层为隐藏层时
            //获得当前层所有节点大于零小于零的排列组合，递归调用计算下一层
            int relu_tag = (int) Math.pow(2, input_layer.length - 1) - 1;
            for (int i = relu_tag; i >= 0; --i){
                //记录当前已有的所有约束
                List<double[]> tmp_condition = new ArrayList<>(condition);
                tmp_condition.addAll(attainCondition(input_layer, i));
                //递归调用实现动态规划
                calculateNet(calculateLayer(cache, i), weights, layer_index, layer_nubmer, tmp_condition);
            }
        }
    }

    /**
     * 计算每一层节点的线性表达式
     * @param cache 存储当前一层所有节点线性表达式大于0时该层节点的计算结果
     * @param relu_tag 前一层各个节点线性表达式与0的大小关系
     * @return 该层各个节点的线性表达式
     */
    private double[][] calculateLayer(double[][][][] cache, int relu_tag){
        double[][] output_layer = new double[cache.length + 1][cache[0][0][0].length];

        for (int i = 0;i < output_layer.length - 1; i++){
            output_layer[i] = calculateNode(cache, i, relu_tag);
        }
        //添加值为1的截距项节点
        output_layer[output_layer.length - 1][output_layer[0].length - 1] = 1;

        return output_layer;
    }

    /**
     * 计算节点的线性表达式
     * @param cache 存储当前一层所有节点线性表达式大于0时该层节点的计算结果
     * @param output_node_index 指明节点序号
     * @param relu_tag 该层各个节点的线性表达式
     * @return 该节点的线性表达式
     */
    private double[] calculateNode(double[][][][] cache, int output_node_index, int relu_tag){
        int length = cache[0].length;

        //考虑多个输入的情况，node元素个数应该为每组输入数据元素个数加1
        double[] node = new double[cache[0][0][0].length];

        node = addDoubleArray(node, cache[output_node_index][length - 1][1]);
        //将十六进制的relu_tag转变为二进制
        for (int i = 0; i < length - 1; ++i){
            boolean isOne = (relu_tag % 2 == 1);
            relu_tag /= 2;
            if(isOne){
                //该位为1时，代表节点的线性表达式大于0
                node = addDoubleArray(node, cache[output_node_index][i][0]);
            }else{
                //该位为0时，代表节点的线性表达式小于0
                node = addDoubleArray(node, cache[output_node_index][i][1]);
            }
        }

        return node;
    }

    private List<double[]> attainCondition(double[][] input_layer, int relu_tag){
        List<double[]> result = new ArrayList<>();
        for (int i = 0; i < input_layer.length - 1; ++i){
            boolean isOne = (relu_tag % 2 == 1);
            relu_tag /= 2;
            if(isOne){
                double[] cache = new double[input_layer[i].length];
                for (int j = 0; j < cache.length; ++j){
                    cache[j] = -input_layer[i][j];
                }
                result.add(cache);
            }else{
                result.add(input_layer[i]);
            }
        }

        return result;
    }

    private double[][][][] attainNodeCache(double[][] input_layer, double[][] weight){
        double[][][][] cache = new double[weight.length][input_layer.length][2][input_layer[0].length];
        int node_last_index = input_layer[0].length - 1;
        for (int i = 0; i < weight.length; ++i){
            for (int j = 0; j < input_layer.length; ++j){
                for (int k = 0; k <= node_last_index; ++k){
                    cache[i][j][1][k] = add(cache[i][j][1][k], multiply(input_layer[j][k], weight[i][j]));
                }
            }
        }

        return cache;
    }

    private int calculateInputNumber(List<double[][]> weights){
        int input_number = 0;
        if(weights != null && weights.size() > 0){
            input_number = weights.get(0)[0].length - 1;
        }
        return input_number;
    }

    private double multiply(double a, double b){
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.multiply(bd2).doubleValue();
    }

    private double add(double a, double b){
        BigDecimal bd1 = new BigDecimal(Double.toString(a));
        BigDecimal bd2 = new BigDecimal(Double.toString(b));
        return bd1.add(bd2).doubleValue();
    }

    private double[] addDoubleArray(double[] a, double[] b){
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; ++i)
            result[i] = add(a[i], b[i]);
        return result;
    }
}
package com.hartwig.hmftools.common.sigs;

import static com.hartwig.hmftools.common.sigs.DataUtils.doublesEqual;

import java.util.List;

import com.google.common.collect.Lists;

public class VectorUtils
{
    public static double sumVector(double[] vec)
    {
        double total = 0;
        for (double v : vec)
        {
            total += v;
        }

        return total;
    }

    public static void sumVectors(final double[] source, double[] dest)
    {
        if(source.length != dest.length)
            return;

        for(int i = 0; i < source.length; ++i)
        {
            dest[i] += source[i];
        }
    }

    public static double[] vectorMultiply(final double[] vec1, final double[] vec2)
    {
        if(vec1.length != vec2.length)
            return null;

        double[] output = new double[vec1.length];
        for(int i = 0; i < vec1.length; ++i)
        {
            output[i] = vec1[i] * vec2[i];
        }

        return output;
    }

    public static void initVector(double[] vec, double value)
    {
        for(int i = 0; i < vec.length; ++i)
        {
            vec[i] = value;
        }
    }

    public static boolean equalVector(double[] vec1, double[] vec2)
    {
        for(int i = 0; i < vec1.length; ++i)
        {
            if(!doublesEqual(vec1[i], vec2[i]))
                return false;
        }

        return true;
    }

    public static void vectorMultiply(double[] vec, double value)
    {
        for(int i = 0; i < vec.length; ++i)
        {
            vec[i] *= value;
        }
    }

    public static void copyVector(final double[] source, double[] dest)
    {
        if(source.length != dest.length)
            return;

        for(int i = 0; i < source.length; ++i)
        {
            dest[i] = source[i];
        }
    }

    public static void addVector(final double[] source, double[] dest)
    {
        if(source.length != dest.length)
            return;

        for(int i = 0; i < source.length; ++i)
        {
            dest[i] += source[i];
        }
    }

    public static List<Integer> getSortedVectorIndices(final double[] data, boolean ascending)
    {
        // returns a list of indices into the original vector, being the sorted data list
        List<Integer> sortedList = Lists.newArrayList();

        for(int i = 0; i < data.length; ++i)
        {
            if(i == 0) {
                sortedList.add(i);
                continue;
            }

            int j = 0;
            for(; j < sortedList.size(); ++j)
            {
                int origIndex = sortedList.get(j);

                if(ascending && data[i] < data[origIndex])
                    break;
                else if(!ascending && data[i] > data[origIndex])
                    break;
            }

            sortedList.add(j, i);
        }

        return sortedList;
    }
}

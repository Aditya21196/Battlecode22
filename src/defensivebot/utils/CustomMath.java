package defensivebot.utils;

public class CustomMath {

    public static int ceilDivision(int dividend, int divisor){
        if(divisor == 0)throw new RuntimeException("division by zero");
        return (dividend+divisor-1)/divisor;
    }

}

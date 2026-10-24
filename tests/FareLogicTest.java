import com.abaho.farecollector.FareLogic;
import java.util.*;

public class FareLogicTest {
    private static int checks=0;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void price(String raw,long low,long high){FareLogic.Price p=FareLogic.parsePrice(raw);check(p!=null&&p.low==low&&p.high==high,"Parse "+raw);}
    static List<FareLogic.Node> nodes(String... pairs){List<FareLogic.Node> out=new ArrayList<>();for(int i=0;i<pairs.length;i+=2)out.add(new FareLogic.Node(pairs[i],pairs[i+1]));return out;}
    public static void main(String[] args){
        price("UGX 12,000",12000,12000);price("2,500-3,500",2500,3500);price("UGX 12,000 – UGX 12,500",12000,12500);
        price("13000",13000,13000);price("4,000 USh",4000,4000);price("UGX\u00a02,000",2000,2000);
        for(String bad:new String[]{"","0","25,000-23,000","5 min","1.7 km","10% off","UGX 12,000 10,000","UGX 2,500,00","-500","UGX 3,000.50","9999999"})check(FareLogic.parsePrice(bad)==null,"Reject "+bad);
        for(String blocked:new String[]{"Request ride","Book now","Confirm pickup","Pay","Cancel ride","Select Bolt","Choose Faras","Ride now","Continue","Find a driver","Next"})check(!FareLogic.safeClick(blocked,""),"Block "+blocked);
        check(!FareLogic.safeClick("",""),"Block unlabeled control");check(FareLogic.safeClick("Where to?",""),"Allow route navigation");
        check(FareLogic.allowedPackage("ee.mtakso.client"),"Known app");check(!FareLogic.allowedPackage("com.bank.app"),"Exclude other apps");
        check(FareLogic.routePresent("From Test Pickup to Test Destination","Test pickup"),"Matching route");
        check(!FareLogic.routePresent("Test Destination","Another pickup"),"Do not invent route verification");
        List<FareLogic.Quote> found=FareLogic.extract(nodes("0/0/0","Saver","0/0/1","UGX 3,000-4,000","0/1/0","Plus","0/1/1","UGX 4,000-5,000"),FareLogic.defaults(),"SafeBoda","Boda");
        check(found.size()==2,"Two separate category rows");check(found.get(0).price.low==3000&&found.get(1).price.low==4000,"Correct row association");
        check(FareLogic.extract(nodes("0/0","Bolt Boda","0/1","Faras Boda","0/2","UGX 3,000"),FareLogic.defaults(),"SafeBoda","Boda").isEmpty(),"No other provider prices");
        check(FareLogic.extract(nodes("0/0/0","Saver","0/0/1","UGX 4,000","0/0/2","UGX 3,000"),FareLogic.defaults(),"SafeBoda","Boda").isEmpty(),"Ambiguous promotional prices require inspection");
        check(FareLogic.extract(nodes("0/0/0","Saver","0/0/1","4 min"),FareLogic.defaults(),"SafeBoda","Boda").isEmpty(),"ETA is not price");
        check(FareLogic.extract(nodes("0/0/0","Saver","0/0/1","UGX 3,000","0/1/0","Saver","0/1/1","UGX 4,000"),FareLogic.defaults(),"SafeBoda","Boda").isEmpty(),"Conflicting duplicate products are not guessed");
        check(FareLogic.activeTripOrLogin("Driver arriving in 3 min"),"Pause active ride");check(FareLogic.activeTripOrLogin("Enter verification code"),"Pause OTP screen");
        check(FareLogic.foreignCurrency("KES 2,500"),"Do not label Kenyan shillings UGX");check(!FareLogic.foreignCurrency("UGX 2,500"),"UGX screen accepted");
        check(FareLogic.PROVIDERS.length==9,"Nine supported providers from the sheet");
        check(!java.util.Arrays.asList(FareLogic.PROVIDERS).contains("Uber"),"Uber remains excluded");
        check(FareLogic.allowedPackage("com.unionapp.rider"),"Recognize the earlier Union passenger app");
        for(FareLogic.Product p:FareLogic.defaults()) {
            java.util.List<FareLogic.Node> row=java.util.Arrays.asList(new FareLogic.Node("0/0",p.label),new FareLogic.Node("0/1","UGX 4,500"));
            java.util.List<FareLogic.Quote> categoryQuotes=FareLogic.extract(row,FareLogic.defaults(),p.provider,p.mode);
            check(categoryQuotes.size()==1&&categoryQuotes.get(0).product.id.equals(p.id),"Read configured category "+p.id);
        }
        System.out.println("PASS: "+checks+" fare parsing, category association, route and action guard checks");
    }
}

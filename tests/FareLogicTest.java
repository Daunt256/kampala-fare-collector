import com.abaho.farecollector.FareLogic;
import java.util.*;

public class FareLogicTest {
    private static int checks=0;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void price(String raw,long low,long high){FareLogic.Price p=FareLogic.parsePrice(raw);check(p!=null&&p.low==low&&p.high==high,"Parse "+raw);}
    static List<FareLogic.Node> nodes(String... pairs){List<FareLogic.Node> out=new ArrayList<>();for(int i=0;i<pairs.length;i+=2)out.add(new FareLogic.Node(pairs[i],pairs[i+1]));return out;}
    static void quote(List<FareLogic.Quote> quotes,String id,long low,long high){
        FareLogic.Quote found=null;
        for(FareLogic.Quote q:quotes)if(q.product.id.equals(id))found=q;
        check(found!=null&&found.price.low==low&&found.price.high==high,"Retain both bounds for "+id);
        check(found!=null&&found.price.raw.contains("~"),"Retain original observed range for "+id);
    }
    public static void main(String[] args){
        price("UGX 2,800 ~ 3,200",2800,3200);
        price("UGX 3,300 ~ 3,700",3300,3700);
        price("UGX 12,000 ~ 12,300",12000,12300);
        price("UGX 14,000 ~ 14,300",14000,14300);
        price("UGX 15,000 ~ 15,300",15000,15300);
        price("UGX 2,800～3,200",2800,3200);price("UGX 2,800〜3,200",2800,3200);
        price("UGX\u00a02,800 ~\n3,200",2800,3200);
        List<FareLogic.Price> embedded=FareLogic.pricesIn("Saver 5+ Mins UGX 2,800 ~ 3,200");
        check(embedded.size()==1&&embedded.get(0).low==2800&&embedded.get(0).high==3200,"Embedded range must not become its lower endpoint");
        for(String invalid:new String[]{"UGX 3,200 ~ 2,800","UGX 2,800 ~","UGX 2,800 ~ 3,200 ~ 4,000"})
            check(FareLogic.pricesIn(invalid).isEmpty(),"Do not salvage a partial fare from "+invalid);
        check(FareLogic.receiverFare("UGX 2,800 ~ 3,200").equals("UGX 2,800 - 3,200"),"Compatible upload keeps both amounts");
        check(FareLogic.receiverFare("UGX 12,000").equals("UGX 12,000"),"Existing fixed-price upload unchanged");
        List<FareLogic.Quote> boda=FareLogic.extract(nodes(
            "0/0/0","Saver","0/0/1","5+ Mins","0/0/2","UGX 2,800 ~ 3,200",
            "0/1/0","Insured Plus","0/1/1","5+ Mins","0/1/2","UGX 3,300 ~ 3,700","0/1/3","Better quality. Top rated SafeBoda drivers",
            "0/2/0","Electric Boda","0/2/1","5+ Mins","0/2/2","UGX 2,800 ~ 3,200"),FareLogic.defaults(),"SafeBoda","Boda");
        check(boda.size()==3,"Read all three SafeBoda Boda categories");
        quote(boda,"sb_saver",2800,3200);quote(boda,"sb_plus",3300,3700);quote(boda,"sb_ev",2800,3200);
        List<FareLogic.Quote> car=FareLogic.extract(nodes(
            "0/0/0","SafeCar","0/0/1","3","0/0/2","10+ Mins","0/0/3","UGX 12,000 ~ 12,300",
            "0/1/0","Comfort","0/1/1","3","0/1/2","10+ Mins","0/1/3","UGX 14,000 ~ 14,300",
            "0/2/0","SafeCar XL","0/2/1","5","0/2/2","10+ Mins","0/2/3","UGX 15,000 ~ 15,300"),FareLogic.defaults(),"SafeBoda","Car");
        check(car.size()==3,"Keep all three SafeCar categories");
        quote(car,"sb_car",12000,12300);quote(car,"sb_comfort",14000,14300);quote(car,"sb_xl",15000,15300);
        List<FareLogic.Quote> merged=FareLogic.extract(nodes(
            "0/0","Saver 5+ Mins UGX 2,800 ~ 3,200",
            "0/1","Insured Plus 5+ Mins UGX 3,300 ~ 3,700",
            "0/2","Electric Boda 5+ Mins UGX 2,800 ~ 3,200"),FareLogic.defaults(),"SafeBoda","Boda");
        check(merged.size()==3,"Read merged accessibility labels with ETA");
        quote(merged,"sb_saver",2800,3200);quote(merged,"sb_plus",3300,3700);quote(merged,"sb_ev",2800,3200);
        List<FareLogic.Quote> mergedCar=FareLogic.extract(nodes(
            "0/0","SafeCar 3 10+ Mins UGX 12,000 ~ 12,300",
            "0/1","Comfort 3 10+ Mins UGX 14,000 ~ 14,300",
            "0/2","SafeCar XL 5 10+ Mins UGX 15,000 ~ 15,300"),FareLogic.defaults(),"SafeBoda","Car");
        check(mergedCar.size()==3,"Read merged car labels without confusing XL and base");
        quote(mergedCar,"sb_car",12000,12300);quote(mergedCar,"sb_comfort",14000,14300);quote(mergedCar,"sb_xl",15000,15300);
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

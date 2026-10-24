package com.abaho.farecollector;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Pure Java parsing and action guards; shared with the local regression tests. */
public final class FareLogic {
    private FareLogic() {}
    public static final String[] PROVIDERS = {"SafeBoda", "Bolt", "Faras", "Lolo Cab", "Konnecta", "Little", "Ride Now", "Union", "Tinka"};
    public static final String[] PACKAGES = {"com.safeboda.passenger", "ee.mtakso.client", "com.faras.rider", "br.com.lolo.ride.passenger", "com.veritalabs.konnectaClient", "com.craftsilicon.littlecabrider", "com.codebase.boda.passenger", "com.rider.unionapp", "com.unionapp.rider", "com.tinkauser.user"};
    private static final String NUM = "(?:\\d{1,3}(?:,\\d{3})+|\\d{3,7})";
    private static final String CUR = "(?:UGX|UShs?|Shs?)";
    private static final String RANGE = "[-–—~〜～]";
    private static final Pattern PRICE = Pattern.compile("^\\s*(?:"+CUR+"\\s*)?("+NUM+")(?:\\s*"+RANGE+"\\s*(?:"+CUR+"\\s*)?("+NUM+"))?\\s*(?:"+CUR+")?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMBEDDED = Pattern.compile("\\b"+CUR+"\\s*"+NUM+"(?:\\s*"+RANGE+"\\s*(?:"+CUR+"\\s*)?"+NUM+")?(?![\\d,.]|\\s*"+RANGE+")", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORBIDDEN = Pattern.compile("\\b(request|book|confirm|order|pay|payment|purchase|checkout|accept|cancel\\s+(?:ride|trip)|start\\s+(?:ride|trip))\\b", Pattern.CASE_INSENSITIVE);
    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
    public static boolean allowedPackage(String p) { return Arrays.asList(PACKAGES).contains(p); }
    public static String[] packagesFor(String provider) {
        if("SafeBoda".equals(provider))return new String[]{"com.safeboda.passenger"};
        if("Bolt".equals(provider))return new String[]{"ee.mtakso.client"};
        if("Faras".equals(provider))return new String[]{"com.faras.rider"};
        if("Lolo Cab".equals(provider))return new String[]{"br.com.lolo.ride.passenger"};
        if("Konnecta".equals(provider))return new String[]{"com.veritalabs.konnectaClient"};
        if("Little".equals(provider))return new String[]{"com.craftsilicon.littlecabrider"};
        if("Ride Now".equals(provider))return new String[]{"com.codebase.boda.passenger"};
        if("Union".equals(provider))return new String[]{"com.rider.unionapp","com.unionapp.rider"};
        if("Tinka".equals(provider))return new String[]{"com.tinkauser.user"};
        throw new IllegalArgumentException("Unknown provider");
    }
    public static String packageFor(String provider) {return packagesFor(provider)[0];}
    public static boolean safeClick(String text, String description) {
        String s = (text == null ? "" : text)+" "+(description == null ? "" : description);
        if (norm(s).isEmpty() || s.length()>350 || FORBIDDEN.matcher(s).find()) return false;
        String n = norm(s);
        if (n.matches("(?:select|choose|ride|go|get)\\s+(?:a\\s+)?(?:bolt|faras|safeboda|safecar|lolo|konnecta|little|union|tinka|boda|car)(?:\\s+.*)?")
            ||n.matches("(?:ride now|let s go|find (?:a )?driver|search (?:for )?(?:a )?driver|continue|next|proceed)(?: .*)?")) return false;
        return true;
    }
    public static boolean activeTripOrLogin(String text) {
        String n=norm(text);
        return n.matches(".*\\b(driver arriving|driver is arriving|trip in progress|ride in progress|cancel ride|cancel trip|enter otp|verification code|enter password|enter your pin|log in|sign in|one time code)\\b.*");
    }
    public static boolean foreignCurrency(String text) {
        return Pattern.compile("\\b(?:KES|KSH|TZS|USD|EUR|GBP|RWF)\\b|[$€£]",Pattern.CASE_INSENSITIVE).matcher(text==null?"":text).find();
    }
    public static final class Price {
        public final long low, high; public final String raw;
        Price(long low,long high,String raw) { this.low=low;this.high=high;this.raw=raw; }
    }
    public static Price parsePrice(String raw) {
        if (raw == null || raw.length()>100) return null;
        Matcher m=PRICE.matcher(raw.replace('\u00a0',' '));
        if (!m.matches()) return null;
        try {
            long low=Long.parseLong(m.group(1).replace(",",""));
            long high=m.group(2)==null?low:Long.parseLong(m.group(2).replace(",",""));
            if (low<100 || high>2000000 || low>high) return null;
            return new Price(low,high,raw.trim());
        } catch (NumberFormatException e) { return null; }
    }
    public static List<Price> pricesIn(String text) {
        List<Price> out=new ArrayList<>();
        Price direct=parsePrice(text);
        if (direct!=null) { out.add(direct); return out; }
        if (text == null || text.length()>500) return out;
        Matcher m=EMBEDDED.matcher(text);
        while (m.find()) { Price p=parsePrice(m.group());if(p!=null)out.add(p); }
        return out;
    }
    /** The deployed 0.2 receiver accepts dashes. Retain the observed text locally and in evidence. */
    public static String receiverFare(String raw) {
        if(parsePrice(raw)==null)throw new IllegalArgumentException("Invalid fare text.");
        return raw.replace('~','-').replace('〜','-').replace('～','-');
    }
    public static final class Product {
        public final String id,label,provider,mode; public final List<String> aliases;
        public Product(String id,String label,String provider,String mode,String... aliases) {
            this.id=id;this.label=label;this.provider=provider;this.mode=mode;this.aliases=Arrays.asList(aliases);
        }
    }
    public static List<Product> defaults() {
        return Arrays.asList(
            new Product("sb_saver","SafeBoda Saver","SafeBoda","Boda","SafeBoda Saver","Saver"),
            new Product("sb_plus","SafeBoda Plus","SafeBoda","Boda","SafeBoda Plus","Insured Plus","Plus"),
            new Product("sb_car","SafeCar","SafeBoda","Car","SafeCar"),
            new Product("sb_xl","SafeCar XL","SafeBoda","Car","SafeCar XL"),
            new Product("bolt_boda","Bolt Boda","Bolt","Boda","Bolt Boda","Boda"),
            new Product("bolt_car","Bolt","Bolt","Car","Bolt"),
            new Product("bolt_xl","Bolt XL","Bolt","Car","Bolt XL","XL"),
            new Product("faras_boda","Faras Boda","Faras","Boda","Faras Boda","Boda"),
            new Product("faras_car","Faras","Faras","Car","Faras"),
            new Product("faras_xl","Faras XL","Faras","Car","Faras XL","XL"),
            new Product("sb_comfort","SafeCar Comfort","SafeBoda","Car","SafeCar Comfort","Comfort"),
            new Product("faras_economy","Faras Economy","Faras","Car","Faras Economy","Economy"),
            new Product("lolo_cab","Lolo Cab","Lolo Cab","Car","Lolo Cab","Lolo"),
            new Product("lolo_xtra","Lolo Xtra","Lolo Cab","Car","Lolo Xtra","Xtra"),
            new Product("lolo_cashless","Go Cashless","Lolo Cab","Car","Go Cashless"),
            new Product("lolo_corporate","Corporate","Lolo Cab","Car","Corporate"),
            new Product("lolo_boda","Lolo Boda","Lolo Cab","Boda","Lolo Boda","Boda"),
            new Product("konnecta_car","Konnecta","Konnecta","Car","Konnecta"),
            new Product("konnecta_boda","Konnecta Boda","Konnecta","Boda","Konnecta Boda","Boda"),
            new Product("little_basic","Little Basic","Little","Car","Little Basic","Basic"),
            new Product("little_comfort","Little Comfort","Little","Car","Little Comfort","Comfort"),
            new Product("little_boda","Little Boda","Little","Boda","Little Boda","Boda"),
            new Product("ridenow_car","Ride Now","Ride Now","Car","Ride Now","RideNow"),
            new Product("ridenow_xl","Ride Now XL","Ride Now","Car","Ride Now XL","RideNow XL","XL"),
            new Product("ridenow_boda","Ride Now Boda","Ride Now","Boda","Ride Now Boda","RideNow Boda","Boda"),
            new Product("union_car","Union car","Union","Car","Union car","Union"),
            new Product("union_max","Union max","Union","Car","Union max","Max"),
            new Product("union_boda","Union boda","Union","Boda","Union boda","Boda"),
            new Product("tinka_car","Tinka","Tinka","Car","Tinka","Tinka Go"),
            new Product("tinka_xl","Tinka XL","Tinka","Car","Tinka XL","XL"),
            new Product("tinka_boda","Tinka Boda","Tinka","Boda","Tinka Boda","Boda"),
            new Product("sb_ev","Electric Boda","SafeBoda","Boda","Electric Boda","SafeBoda EV","EV")
        );
    }
    public static final class Node {
        public final String path,text;
        public Node(String path,String text) { this.path=path;this.text=text==null?"":text; }
    }
    public static final class Quote {
        public final Product product; public final Price price; public final String evidence;
        Quote(Product product,Price price,String evidence) {this.product=product;this.price=price;this.evidence=evidence;}
    }
    private static Product productIn(String text,List<Product> products) {
        String label=text==null?"":text;
        Matcher m=EMBEDDED.matcher(label);
        if(m.find()) label=label.substring(0,m.start());
        // Some accessibility nodes merge the category, seats and ETA into one label.
        String n=norm(label).replaceFirst("(?: \\d{1,2})? \\d{1,3} (?:mins?|minutes?)$","");
        Product found=null;
        for(Product p:products) for(String alias:p.aliases) if(n.equals(norm(alias))) {
            if(found!=null&&!found.id.equals(p.id)) return null;
            found=p;
        }
        return found;
    }
    private static String parent(String path) {int i=path.lastIndexOf('/');return i<0?"":path.substring(0,i);}
    private static boolean within(String path,String scope) {return path.equals(scope)||path.startsWith(scope+"/");}
    /** Only associate a price when the nearest row contains one product and one unique fare. */
    public static List<Quote> extract(List<Node> nodes,List<Product> catalog,String provider,String mode) {
        List<Product> products=new ArrayList<>();
        for(Product p:catalog)if(p.provider.equals(provider)&&p.mode.equals(mode))products.add(p);
        Map<String,Quote> result=new LinkedHashMap<>(); Set<String> conflicts=new HashSet<>();
        for(Node node:nodes) {
            Product p=productIn(node.text,products); if(p==null)continue;
            String scope=node.path;
            for(int level=0;level<3;level++,scope=parent(scope)) {
                if(scope.isEmpty())break;
                Set<String> productIds=new HashSet<>(); Map<String,Price> fares=new LinkedHashMap<>(); StringBuilder evidence=new StringBuilder();
                for(Node sibling:nodes) if(within(sibling.path,scope)) {
                    Product other=productIn(sibling.text,products); if(other!=null)productIds.add(other.id);
                    for(Price price:pricesIn(sibling.text)) fares.put(price.low+":"+price.high,price);
                    if(!sibling.text.isEmpty()&&evidence.length()<300)evidence.append(sibling.text).append(" | ");
                }
                if(productIds.size()>1)break;
                if(productIds.size()==1&&fares.size()==1) {
                    Price price=fares.values().iterator().next(); Quote old=result.get(p.id);
                    if(old!=null&&(old.price.low!=price.low||old.price.high!=price.high))conflicts.add(p.id);
                    result.put(p.id,new Quote(p,price,evidence.toString())); break;
                }
                if(fares.size()>1)break; // e.g. old and promotional prices: require review, never guess.
            }
        }
        for(String id:conflicts)result.remove(id);
        return new ArrayList<>(result.values());
    }
    public static boolean routePresent(String visible,String query) {
        String hay=" "+norm(visible)+" "; String needle=norm(query);
        if(needle.length()<5)return false;
        if(hay.contains(" "+needle+" "))return true;
        String[] tokens=needle.split(" ");int meaningful=0,matched=0;
        Set<String> ignore=new HashSet<>(Arrays.asList("the","road","rd","kampala","uganda","plot","street","st"));
        for(String token:tokens) if(token.length()>=4&&!ignore.contains(token)) {
            meaningful++; if(hay.contains(" "+token+" "))matched++;
        }
        return meaningful>=2&&matched==meaningful;
    }
}

package models.seed;

import io.ebean.DB;
import models.sql.City;
import models.sql.States;

/**
 * Seeds Indian states and major cities into the DB.
 * Safe to call multiple times - skips if data already exists.
 */
public class LocationSeeder {

    public static void seed() {
        boolean statesExist = DB.find(States.class).findCount() > 0;
        int cityCount = DB.find(City.class).findCount();

        if (statesExist && cityCount >= 100) {
            System.out.println("[LocationSeeder] States and cities already seeded, skipping.");
            return;
        }

        if (!statesExist) {
            System.out.println("[LocationSeeder] Seeding states...");
            seedStates();
        }

        if (cityCount < 100) {
            System.out.println("[LocationSeeder] Cities incomplete (" + cityCount + "), clearing and re-seeding...");
            DB.sqlUpdate("DELETE FROM cities").execute();
            seedCities();
        }

        System.out.println("[LocationSeeder] Done. " + DB.find(States.class).findCount() + " states, " + DB.find(City.class).findCount() + " cities.");
    }

    private static States createState(String name, String code) {
        States s = new States();
        s.name = name;
        s.code = code;
        s.save();
        return s;
    }

    private static void createCity(String name, long stateId) {
        City c = new City();
        c.name = name;
        c.stateId = stateId;
        c.save();
    }

    private static void seedStates() {
        createState("Andhra Pradesh", "AP");
        createState("Arunachal Pradesh", "AR");
        createState("Assam", "AS");
        createState("Bihar", "BR");
        createState("Chhattisgarh", "CG");
        createState("Goa", "GA");
        createState("Gujarat", "GJ");
        createState("Haryana", "HR");
        createState("Himachal Pradesh", "HP");
        createState("Jharkhand", "JH");
        createState("Karnataka", "KA");
        createState("Kerala", "KL");
        createState("Madhya Pradesh", "MP");
        createState("Maharashtra", "MH");
        createState("Manipur", "MN");
        createState("Meghalaya", "ML");
        createState("Mizoram", "MZ");
        createState("Nagaland", "NL");
        createState("Odisha", "OD");
        createState("Punjab", "PB");
        createState("Rajasthan", "RJ");
        createState("Sikkim", "SK");
        createState("Tamil Nadu", "TN");
        createState("Telangana", "TS");
        createState("Tripura", "TR");
        createState("Uttar Pradesh", "UP");
        createState("Uttarakhand", "UK");
        createState("West Bengal", "WB");
        // Union Territories
        createState("Delhi", "DL");
        createState("Chandigarh", "CH");
        createState("Jammu and Kashmir", "JK");
        createState("Ladakh", "LA");
        createState("Puducherry", "PY");
        createState("Andaman and Nicobar Islands", "AN");
        createState("Dadra Nagar Haveli and Daman Diu", "DN");
        createState("Lakshadweep", "LD");
    }

    private static long stateId(String code) {
        States s = DB.find(States.class).where().eq("code", code).findOne();
        if (s == null) throw new RuntimeException("State not found: " + code);
        return s.getId();
    }

    private static void seedCities() {
        // Andhra Pradesh
        long ap = stateId("AP");
        for (String c : new String[]{"Visakhapatnam", "Vijayawada", "Guntur", "Nellore", "Kurnool", "Tirupati", "Rajahmundry", "Kakinada", "Kadapa", "Anantapur", "Eluru", "Ongole", "Srikakulam", "Chittoor", "Machilipatnam"}) createCity(c, ap);

        // Arunachal Pradesh
        long ar = stateId("AR");
        for (String c : new String[]{"Itanagar", "Naharlagun", "Tawang", "Ziro", "Pasighat", "Bomdila"}) createCity(c, ar);

        // Assam
        long as_ = stateId("AS");
        for (String c : new String[]{"Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Nagaon", "Tinsukia", "Tezpur", "Bongaigaon", "Karimganj", "Diphu"}) createCity(c, as_);

        // Bihar
        long br = stateId("BR");
        for (String c : new String[]{"Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Darbhanga", "Purnia", "Arrah", "Bihar Sharif", "Begusarai", "Katihar", "Munger", "Chapra", "Saharsa", "Hajipur", "Sasaram"}) createCity(c, br);

        // Chhattisgarh
        long cg = stateId("CG");
        for (String c : new String[]{"Raipur", "Bhilai", "Bilaspur", "Korba", "Durg", "Rajnandgaon", "Raigarh", "Jagdalpur", "Ambikapur"}) createCity(c, cg);

        // Goa
        long ga = stateId("GA");
        for (String c : new String[]{"Panaji", "Margao", "Vasco da Gama", "Mapusa", "Ponda"}) createCity(c, ga);

        // Gujarat
        long gj = stateId("GJ");
        for (String c : new String[]{"Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar", "Junagadh", "Gandhinagar", "Anand", "Nadiad", "Morbi", "Mehsana", "Bharuch", "Vapi", "Navsari", "Veraval", "Porbandar", "Godhra", "Palanpur", "Gandhidham"}) createCity(c, gj);

        // Haryana
        long hr = stateId("HR");
        for (String c : new String[]{"Gurugram", "Faridabad", "Panipat", "Ambala", "Karnal", "Hisar", "Rohtak", "Sonipat", "Panchkula", "Yamunanagar", "Bhiwani", "Sirsa", "Rewari", "Kurukshetra"}) createCity(c, hr);

        // Himachal Pradesh
        long hp = stateId("HP");
        for (String c : new String[]{"Shimla", "Dharamshala", "Mandi", "Solan", "Kullu", "Bilaspur", "Hamirpur", "Una", "Palampur", "Manali"}) createCity(c, hp);

        // Jharkhand
        long jh = stateId("JH");
        for (String c : new String[]{"Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Deoghar", "Hazaribagh", "Giridih", "Ramgarh", "Dumka"}) createCity(c, jh);

        // Karnataka
        long ka = stateId("KA");
        for (String c : new String[]{"Bengaluru", "Mysuru", "Hubballi", "Mangaluru", "Belgaum", "Davanagere", "Bellary", "Gulbarga", "Shimoga", "Tumkur", "Udupi", "Raichur", "Hassan", "Bidar", "Hospet"}) createCity(c, ka);

        // Kerala
        long kl = stateId("KL");
        for (String c : new String[]{"Thiruvananthapuram", "Kochi", "Kozhikode", "Thrissur", "Kollam", "Kannur", "Alappuzha", "Palakkad", "Kottayam", "Malappuram"}) createCity(c, kl);

        // Madhya Pradesh
        long mp = stateId("MP");
        for (String c : new String[]{"Bhopal", "Indore", "Jabalpur", "Gwalior", "Ujjain", "Sagar", "Dewas", "Satna", "Ratlam", "Rewa", "Murwara", "Singrauli", "Burhanpur", "Khandwa", "Morena", "Bhind", "Chhindwara", "Guna", "Shivpuri", "Vidisha", "Damoh", "Mandsaur", "Khargone", "Neemuch", "Pithampur", "Hoshangabad", "Itarsi", "Sehore"}) createCity(c, mp);

        // Maharashtra
        long mh = stateId("MH");
        for (String c : new String[]{"Mumbai", "Pune", "Nagpur", "Thane", "Nashik", "Aurangabad", "Solapur", "Kolhapur", "Amravati", "Navi Mumbai", "Sangli", "Malegaon", "Jalgaon", "Akola", "Latur", "Dhule", "Ahmednagar", "Chandrapur", "Parbhani", "Satara", "Ichalkaranji", "Nanded", "Ratnagiri"}) createCity(c, mh);

        // Manipur
        long mn = stateId("MN");
        for (String c : new String[]{"Imphal", "Thoubal", "Bishnupur", "Churachandpur"}) createCity(c, mn);

        // Meghalaya
        long ml = stateId("ML");
        for (String c : new String[]{"Shillong", "Tura", "Jowai", "Nongpoh"}) createCity(c, ml);

        // Mizoram
        long mz = stateId("MZ");
        for (String c : new String[]{"Aizawl", "Lunglei", "Champhai", "Serchhip"}) createCity(c, mz);

        // Nagaland
        long nl = stateId("NL");
        for (String c : new String[]{"Kohima", "Dimapur", "Mokokchung", "Tuensang", "Wokha"}) createCity(c, nl);

        // Odisha
        long od = stateId("OD");
        for (String c : new String[]{"Bhubaneswar", "Cuttack", "Rourkela", "Berhampur", "Sambalpur", "Puri", "Balasore", "Bhadrak", "Baripada", "Jharsuguda"}) createCity(c, od);

        // Punjab
        long pb = stateId("PB");
        for (String c : new String[]{"Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Mohali", "Hoshiarpur", "Pathankot", "Moga", "Firozpur", "Batala", "Abohar", "Khanna", "Phagwara"}) createCity(c, pb);

        // Rajasthan
        long rj = stateId("RJ");
        for (String c : new String[]{"Jaipur", "Jodhpur", "Kota", "Bikaner", "Ajmer", "Udaipur", "Bhilwara", "Alwar", "Bharatpur", "Sikar", "Pali", "Sri Ganganagar", "Tonk", "Kishangarh", "Beawar", "Hanumangarh", "Chittorgarh", "Barmer"}) createCity(c, rj);

        // Sikkim
        long sk = stateId("SK");
        for (String c : new String[]{"Gangtok", "Namchi", "Gyalshing", "Mangan"}) createCity(c, sk);

        // Tamil Nadu
        long tn = stateId("TN");
        for (String c : new String[]{"Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tirunelveli", "Tiruppur", "Vellore", "Erode", "Thoothukudi", "Dindigul", "Thanjavur", "Ranipet", "Sivakasi", "Karur", "Nagercoil", "Kanchipuram", "Hosur", "Kumbakonam"}) createCity(c, tn);

        // Telangana
        long ts = stateId("TS");
        for (String c : new String[]{"Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam", "Ramagundam", "Mahbubnagar", "Nalgonda", "Adilabad", "Suryapet", "Siddipet", "Miryalaguda"}) createCity(c, ts);

        // Tripura
        long tr = stateId("TR");
        for (String c : new String[]{"Agartala", "Udaipur", "Dharmanagar", "Kailasahar"}) createCity(c, tr);

        // Uttar Pradesh
        long up = stateId("UP");
        for (String c : new String[]{"Lucknow", "Kanpur", "Agra", "Varanasi", "Meerut", "Prayagraj", "Ghaziabad", "Noida", "Bareilly", "Aligarh", "Moradabad", "Gorakhpur", "Saharanpur", "Jhansi", "Mathura", "Firozabad", "Muzaffarnagar", "Shahjahanpur", "Rampur", "Ayodhya", "Etawah", "Fatehpur", "Hapur", "Budaun", "Lakhimpur Kheri", "Unnao"}) createCity(c, up);

        // Uttarakhand
        long uk = stateId("UK");
        for (String c : new String[]{"Dehradun", "Haridwar", "Roorkee", "Haldwani", "Rudrapur", "Kashipur", "Rishikesh", "Kotdwar", "Nainital", "Mussoorie"}) createCity(c, uk);

        // West Bengal
        long wb = stateId("WB");
        for (String c : new String[]{"Kolkata", "Howrah", "Asansol", "Siliguri", "Durgapur", "Bardhaman", "Malda", "Baharampur", "Habra", "Kharagpur", "Shantiniketan", "Haldia", "Raiganj", "Krishnanagar", "Kalyani"}) createCity(c, wb);

        // Delhi
        long dl = stateId("DL");
        for (String c : new String[]{"New Delhi", "Central Delhi", "South Delhi", "North Delhi", "East Delhi", "West Delhi", "Dwarka", "Rohini", "Saket"}) createCity(c, dl);

        // Chandigarh
        long ch = stateId("CH");
        for (String c : new String[]{"Chandigarh", "Manimajra", "Mohali Extension", "Panchkula Extension"}) createCity(c, ch);

        // Jammu and Kashmir
        long jk = stateId("JK");
        for (String c : new String[]{"Srinagar", "Jammu", "Anantnag", "Baramulla", "Sopore", "Kathua", "Udhampur"}) createCity(c, jk);

        // Ladakh
        long la = stateId("LA");
        for (String c : new String[]{"Leh", "Kargil", "Diskit", "Padum"}) createCity(c, la);

        // Puducherry
        long py = stateId("PY");
        for (String c : new String[]{"Puducherry", "Karaikal", "Mahe", "Yanam"}) createCity(c, py);

        // Andaman and Nicobar Islands
        long an = stateId("AN");
        for (String c : new String[]{"Port Blair", "Car Nicobar", "Mayabunder", "Diglipur"}) createCity(c, an);

        // Dadra Nagar Haveli and Daman Diu
        long dn = stateId("DN");
        for (String c : new String[]{"Silvassa", "Daman", "Diu", "Amli"}) createCity(c, dn);

        // Lakshadweep
        long ld = stateId("LD");
        for (String c : new String[]{"Kavaratti", "Agatti", "Minicoy", "Andrott"}) createCity(c, ld);
    }
}

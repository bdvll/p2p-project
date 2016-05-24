package se.kth.news.core.news.util;

public class ClickBaiter {
	
	private static String[] verbsPresent = {"eat", "shoot", "hide", "kidnap", "steal", "sit on", "kill", "kiss", "hit", "buy", "sell","hit on"};
	private static String[] wow ={"amazing", "interesting", "hilarious","incredible","cheeky","illegal"};
	
	private static String[] verbsPast = {"ate", "shot", "hid", "kidnapped", "stole", "sat on", "killed", "kissed", "hit", "bought", "sold","hit on"};
	private static String[] nouns = {"dog","child","senior citizen","elephant","car","house","cat"};
	private static Adjective[] adjectives = { new Adjective("a","cute"), new Adjective("a","huge"),new Adjective("a","fat"),new Adjective("an","old"),new Adjective("a","fast"),new Adjective("a","tiny"),new Adjective("a","sexy")};
	private static String[] adverbs = {"cold-heartedly","passionately","furiously","slowly","gracefully","quickly","quietly"};
	private static String[] subjects = {"A truck driver","A random Italian guy", "The king of Spain", "The guy from Scrubs","I","Göran Persson","Emil Karlsson", "Robert Rönngren","Åsa Romson","Jimmie Åkesson","Carola","Amy Diamond"};
	private static String[] clickBait ={"you can never guess what happened next!", "what happened next will blow your mind!","then something incredible happened!", "with this one weird tip!", "then hell broke loose!","the result is amazing!","what happened next is breaking the internet!","first you will be chocked, then you will be inspired!"};

	public static String getBait() {
		StringBuilder sb = new StringBuilder("");
		if(Math.random() < 0.5){
			int rnd = (int)(Math.random()*30);
			sb.append(Integer.toString(rnd)+" ");
			sb.append(wow[(int)(Math.random()*wow.length)]+" ");
			sb.append("ways to ");
			sb.append(verbsPresent[(int)(Math.random()*verbsPresent.length)]+" ");
			
			int adjective = (int)(Math.random()*adjectives.length);
			sb.append(adjectives[adjective].prefix+" ");
			sb.append(adjectives[adjective].adjective+" ");
			sb.append(nouns[(int)(Math.random()*nouns.length)]+"!");
			int rnd2 =rnd-((int)(Math.random()*rnd)); 
			sb.append(" Number "+rnd2+" will blow your mind!");
		}else{
			
			sb.append(subjects[(int)(Math.random()*subjects.length)]+" ");
			sb.append(adverbs[(int)(Math.random()*adverbs.length)]+" ");
			sb.append(verbsPast[(int)(Math.random()*verbsPast.length)]+" ");
			
			int adjective = (int)(Math.random()*adjectives.length);
			sb.append(adjectives[adjective].prefix+" ");
			sb.append(adjectives[adjective].adjective+" ");
			
			sb.append(nouns[(int)(Math.random()*nouns.length)]);
			sb.append(", ");
			sb.append(clickBait[(int)(Math.random()*clickBait.length)]);
		}
		
		return sb.toString();
	}
	private static class Adjective{
		private String adjective;
		private String prefix;
		
		public Adjective(String prefix, String adjective){
			this.adjective = adjective;
			this.prefix = prefix;
		}
	}

}

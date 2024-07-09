import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;




enum Direction { AVANT, ARRIERE, HAUT, BAS }

enum Action {
    AVANCE("Avance"),
    RECULE("Recule"),
    GRIMPE("Grimpe"),
    DESCEND("Descend"),
    BRAQUE("Braque"),
    TIRE("Tire"),

    DEVANT("Devant"),
    DERRIERE("Derriere"),
    HAUT("Haut"),
    BAS("Bas");

    private final String label;

    Action(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Action convertToAction(String label) {
        for (Action action : values()) {
            if (action.getLabel().equalsIgnoreCase(label)) {
                return action;
            }
        }
        throw new IllegalArgumentException("Unknown label: " + label);
    }
}

interface loot_transfer {
    default LootElement randomtransfer(ArrayList<LootElement> sender,ArrayList<LootElement> destination) {
        int x = sender.size();
        Random random = new Random();
        int r = random.nextInt(x);
        LootElement l = sender.get(r);
        destination.add(sender.remove(r))  ;
        return l;
    }
    default void transfer(ArrayList<LootElement> sender,ArrayList<LootElement> destination) {
        destination.add(sender.getLast());
        sender.removeLast();
    }
}

abstract class LootElement {
    private final String itemName;
    protected int value;

    public LootElement(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }
    public int getValue(){ return value; }

}

class Bourse extends LootElement{
    public Bourse(String name){
        super(name);
        Random random = new Random();
        this.value = random.nextInt(500);
    }
}

class Bijoux extends LootElement{
    public Bijoux(String name){
        super(name);
        this.value = 500;
    }
}

class Magot extends LootElement {
    public Magot(String name){
        super(name);
        this.value = 1000;
    }
}

abstract class TrainElement implements loot_transfer {
    private final int positionIndex;
    private final ArrayList<LootElement> loot;
    private final ArrayList<Bandit> characters;
    boolean Marshall ;

    public TrainElement (int positionIndex){
        this.positionIndex = positionIndex;
        this.loot = new ArrayList<>();
        this.characters = new ArrayList<>();
    }

    public int getPositionIndex() {return positionIndex;}

    public ArrayList<LootElement> getLoot() {return loot;}

    public void addLoot(LootElement lootElement) {loot.add(lootElement);}

    public void newcharacter (Direction d , Character e) {
        if(e instanceof Bandit) {
            switch (d) {
                case ARRIERE:
                    characters.add((Bandit) e);break;
                case AVANT:
                    characters.addFirst((Bandit) e);break;
            }
        }else{
            Marshall = true;
        }
    }

    public ArrayList<Bandit> getCharacters() {
        return characters;
    }
    public Bandit getcharacter(int i){return characters.get(i);}

    public void removeCharacter(Character c){
        if(c instanceof Bandit) {
            characters.remove(c);
        }else{
            Marshall = false;
        }

    }

}

class Wagon extends TrainElement {
    public Wagon(int i) {
        super(i);
    }
}

class Locomotive extends TrainElement {

    public Locomotive(int i) {
        super(i);


    }
}

abstract class Character implements loot_transfer{

    protected String name;
    protected TrainElement x;

    public Character(Train train) {
        this.x = train.TrainElements[0];
    }
    public int getIndex() {
        return  x.getPositionIndex();
    }
    public String getName() {return name;}

    public void setX (TrainElement e) {x = e;}

    public String move(Train train , Direction direction) {

        if(x instanceof Locomotive && direction == Direction.AVANT)
        {
            return(name + "ne peut plus avancer ");
        }

        else if(x.getPositionIndex() == 0 && direction == Direction.ARRIERE){
            return(name + "ne peut pas reculer ");
        }



        switch(direction) {
            case AVANT:
                this.x.removeCharacter(this);
                setX(train.getWagon(x.getPositionIndex() + 1));
                x.newcharacter(Direction.AVANT, this);
                break;
            case ARRIERE:
                this.x.removeCharacter(this);
                setX(train.getWagon(x.getPositionIndex() - 1));
                x.newcharacter(Direction.ARRIERE, this);
                break;
        }

        return(name + " se déplace vers l' " + direction + ".");




    }
}

class Bandit extends Character{
    private boolean isOnTheRoof;

    private final ArrayList<LootElement> myLoot;
    private final ArrayList<Action> actions;

    private int remaining_bullets;
    private int score;
    private int shotsHit;
    private int busted;

    public Bandit(String name, Train train ,int bullets){
        super(train);
        this.name  = name;
        this.isOnTheRoof = true;
        this.remaining_bullets = bullets;
        this.score = 0;
        this.shotsHit = 0;
        this.busted = 0;

        myLoot = new ArrayList<>();
        actions = new ArrayList<>();
        this.x.newcharacter(Direction.AVANT,this);
    }

    public boolean onTheRoof(){ return isOnTheRoof;}


    public String climbRoof() {
        if(isOnTheRoof){
            return (name + " est deja sur le toit.");
        }else {
            this.isOnTheRoof = true;
            return (name + " grimpe sur le toit.");
        }
    }

    public String entreWagon() {
        if(!isOnTheRoof){
            return (name + " est deja dans le wagon.");
        }else {
            isOnTheRoof = false;
            return (name + " entre le wagon.");

        }
    }

    public String braque() {
        ArrayList<LootElement> loot = x.getLoot();

        if ( !loot.isEmpty()) {
            LootElement s = randomtransfer(loot,myLoot);
            score+=s.getValue();
            return (name + " a braqué " + s.getItemName() );
        }
        else {
            return (name + " il ne reste plus rien a braquer");
        }
    }

    /**
    public String tire(Direction d) {

        if (remaining_bullets > 0) {

            remaining_bullets--;

            int index = x.getCharacters().indexOf(this);

            switch (d) {
                case ARRIERE:
                    System.out.println("case arriere");
                    if (index != 0) {
                        for (int i = index - 1; i > 0; i--) {
                            Bandit victim = x.getcharacter(i);
                            if (victim.onTheRoof() == isOnTheRoof) {
                                victim.shot();
                                shotsHit++;
                                return (name + " shot " + victim.getName());
                            }
                        }

                    } else {
                        return (name + "can't shoot back");
                    }

                case AVANT:
                    System.out.println("case avant");
                    for (int i = index; i < x.getCharacters().size() - 1; i++) {
                        Bandit victim = x.getcharacter(i);

                        if (victim.onTheRoof() == isOnTheRoof) {
                            victim.shot();
                            shotsHit++;
                            return (name + " shot " + victim.getName());
                        }

                    }

                    return (name + "gotta fix his aim");


                case HAUT:
                    System.out.println("case haut");
                    if (!onTheRoof()) {
                        for (int i = 0; i < x.getCharacters().size(); i++) {
                            Bandit victim = x.getcharacter(i);
                            if (victim.onTheRoof()) {
                                victim.shot();
                                shotsHit++;
                                return (name + " shot " + victim.getName());
                            }
                        }
                        return (name + "didn't hit anyone");
                    } else {
                        return (name + "shooting the birds?");
                    }

                case BAS:
                    System.out.println("case bas");

                    if (onTheRoof()) {
                        for (int i = 0; i < x.getCharacters().size(); i++) {
                            Bandit victim = x.getcharacter(i);
                            if (!victim.onTheRoof()) {
                                victim.shot();
                                shotsHit++;
                                return (name + " shot " + victim.getName());
                            }
                        }
                        return (name + "didn't hit anyone");
                    } else {
                        return (name + "shot his foot");
                    }
            }

        }

        return (name + "ran out of bullets");

    }
    **/

    public String tire(Direction d){
        if(remaining_bullets>0){
            remaining_bullets--;
            for(Bandit bandit: x.getCharacters()){
                if(bandit != this){
                    if(bandit.onTheRoof() == isOnTheRoof){
                        bandit.shot();
                        shotsHit++;
                        return(name + " shot " + bandit.getName());
                    }
                }
            }
            return (name + " didn't hit anyone");

        }else{
            return (name + "ran out of bullets");

        }
    }



    public int getScore() {return score;}
    public int getRemaining_bullets() {return remaining_bullets;}
    public ArrayList<Action> getActions() {return actions;}

    public void addActions(Action a) {actions.add(a);}

    public void clearActions (){ actions.clear(); }

    public void shot(){
        if(!myLoot.isEmpty()) {
            LootElement s = randomtransfer(this.myLoot,x.getLoot());
            score -= s.getValue();
        }
    }

    public void setOnTheRoof(boolean onTheRoof) {
        isOnTheRoof = onTheRoof;
    }

    public void setBusted(int busted) {
    }

    public int getShotsHit() {
        return shotsHit;
    }

    public void busted() {
        busted++;
    }
}

class Marshall extends Character {
    public float NERVOSITE_MARSHALL = 3;

    public Marshall(Train train) {
        super(train);
        this.x = train.TrainElements[train.TrainElements.length - 1];
        this.name = "Marshall";
    }
    public String controlleBandits() {
        if (!x.getCharacters().isEmpty()) {
            for (Bandit bandit : x.getCharacters()) {
                if(!bandit.onTheRoof()) {
                    bandit.shot();
                    bandit.busted();
                    bandit.setOnTheRoof(true);
                    return ("Marshall shot " + bandit.name);
                }
            }
        }
        return ("Marshall is fighting ghosts");
    }

    public String investigate(Train train){
        Random random = new Random();
        int rand = random.nextInt(10);
        if(rand < NERVOSITE_MARSHALL) {
            move(train, random.nextInt(2) == 0 ? Direction.AVANT : Direction.ARRIERE);
            return controlleBandits();
        }
        return("");
    }

}

class Train {
    TrainElement[] TrainElements;

    public Train (int numWagons) {
        this.TrainElements = new TrainElement[numWagons];

        for (int i = 0 ; i < numWagons-1 ; i++){
            TrainElements[i] = new Wagon(i);
        }

        TrainElements[numWagons-1] = new Locomotive(numWagons-1);

    }

    public TrainElement getWagon (int x) {
        return TrainElements[x];
    }
}


/////////////////MODEL/////////////////////////////////////////////
class ColtModel extends Observable {
    public static final int NB_WAGONS = 6;
    public static final int NB_BULLETS = 6;

    public static int NB_PLAYERS;

    public static final int NB_ACTIONS = 4;

    private Train train;
    private Bandit [] bandits;
    private Marshall marshall;
    private Bandit currentplayer;

    private int turnnumber;

    private ArrayList<String> logs;

    public ColtModel (String[] players) {

        NB_PLAYERS = players.length;

        train = new Train(NB_WAGONS);
        bandits = new Bandit[NB_PLAYERS];
        marshall = new Marshall(train);

        turnnumber = 0 ;
        logs = new ArrayList<>();

        initBandits(NB_PLAYERS,bandits,train,players);
        currentplayer = bandits[turnnumber];

        initLoot(train);
        setChanged();
        notifyObservers(currentplayer);
    }

    private void initLoot(Train train) {
        for ( int i = 0 ; i < train.TrainElements.length - 1; i ++) {
            Random random = new Random();
            int nb = random.nextInt(4) + 1;
            for (int j = 0; j < nb; j++) {
                if(random.nextInt(2) == 1){
                    LootElement loot = new Bijoux("Bijoux");
                    train.TrainElements[i].addLoot(loot);
                }else {
                    LootElement loot = new Bourse("Bourse");
                    train.TrainElements[i].addLoot(loot);
                }
            }
        }

        train.TrainElements[train.TrainElements.length -1].addLoot(new Magot("Magot"));
    }

    private void initBandits(int nbPlayers, Bandit[] bandits, Train train, String[] players) {
        for (int i = 0 ; i < NB_PLAYERS ; i ++ ) {
            bandits[i] = new Bandit(players[i],train,NB_BULLETS);
        }

    }

    public void addBanditAction(Action a) {
        currentplayer.addActions(a);
        setChanged();
        notifyObservers(currentplayer);
    }

    public void nextplayer () {
        turnnumber += 1;
        currentplayer = bandits[turnnumber];

        setChanged();
        notifyObservers(turnnumber);
    }

    public void executeBanditAction(int i, boolean b) {

//        for (int i = 0; i < NB_ACTIONS; i++) {

            for (Bandit bandit : bandits) {
                logs.add(marshall.investigate(train));
                Action a = bandit.getActions().get(i);

                switch (a) {
                    case AVANCE:
                        logs.add(bandit.move(train, Direction.AVANT));
                        break;
                    case RECULE:
                        logs.add(bandit.move(train, Direction.ARRIERE));
                        break;
                    case GRIMPE:
                        logs.add(bandit.climbRoof());
                        break;
                    case BRAQUE:
                        logs.add(bandit.braque());
                        break;
                    case DESCEND:
                        logs.add(bandit.entreWagon());
                        break;
                    case DERRIERE:
                        logs.add(bandit.tire(Direction.ARRIERE));
                        break;
                    case DEVANT:
                        logs.add(bandit.tire(Direction.AVANT));
                        break;
                    case HAUT:
                        logs.add(bandit.tire(Direction.HAUT));
                        break;
                    case BAS:
                        logs.add(bandit.tire(Direction.BAS));
                        break;
                }


                setChanged();
                notifyObservers(train);
                setChanged();
                notifyObservers(bandits);



            }
//        }

        if(b) {
            for (Bandit bandit : bandits) {
                bandit.clearActions();
            }
        }

        setChanged();
        notifyObservers(logs);

        logs.clear();

        turnnumber = 0;
        currentplayer = bandits[turnnumber];

        setChanged();
        notifyObservers(currentplayer);
//        saveBanditStatsToDatabase();
    }

    public void saveBanditStatsToDatabase() {

        String jdbcUrl = "jdbc:mariadb://192.168.1.138:3306/bandit_stats";
        String username = "app";
        String password = "1234";


        String sql = "INSERT INTO bandit_stats (name, score, remaining_bullets) VALUES (?, ?, ?)";

        try (

                Connection connection = DriverManager.getConnection(jdbcUrl, username, password);

                PreparedStatement statement = connection.prepareStatement(sql);
        ) {

            for (Bandit bandit : bandits) {

                statement.setString(1, bandit.getName());
                statement.setInt(2, bandit.getScore());
                statement.setInt(3, bandit.getRemaining_bullets());


                statement.executeUpdate();
            }

            System.out.println("Bandit stats saved to the database successfully.");
        } catch (SQLException e) {
            System.err.println("Error saving bandit stats to the database: " + e.getMessage());
        }
    }


    public TrainElement[] getTrainLayout () { return train.TrainElements;}
    public Bandit[] getBandits () {return bandits;}

    public void clearplayerselection () {
        currentplayer.clearActions();

        setChanged();
        notifyObservers(currentplayer);


    }

    public Marshall getMarshall() {
        return marshall;
    }

    public Bandit getCurrentplayer () {
        return currentplayer;
    }

    public int getTurnnumber () {
        return turnnumber;
    }
}
/////////////////////////////////////////////////////////////////



///////////////VIEW////////////////////////////////////////////
class ColtView extends JPanel {
    private JFrame frame;
    private ConsoleOutputView consoleOutput;

    private Commandsview commands;
    private TrainView train;
    private ScoreBoardView score;



    public ColtView(ColtModel model) {

        frame = new JFrame();
        frame.setTitle("Colt Express");
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(model.NB_WAGONS * 200 +400, 900));

        commands = new Commandsview(model);
        train = new TrainView(model);
        score = new ScoreBoardView(model);
        consoleOutput = new ConsoleOutputView(model);

        frame.add(score, BorderLayout.SOUTH);
        frame.add(commands, BorderLayout.NORTH);
        frame.add(train, BorderLayout.CENTER);
        frame.add(consoleOutput, BorderLayout.EAST);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}


class ConsoleOutputView  extends JPanel implements Observer {
    private JTextArea consoleOutput;

    public ConsoleOutputView(ColtModel model) {
        model.addObserver(this);
        consoleOutput = new JTextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setFont(new Font("Arial", Font.BOLD, 14));
        consoleOutput.append("Fire your bullets wisely and beware of the Marshall.\nGood Luck!\n");

        JScrollPane scrollPane = new JScrollPane(consoleOutput);
        scrollPane.setPreferredSize(new Dimension(400, 900));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof ArrayList<?>) {
            ArrayList<String> logs = (ArrayList<String>) arg;
            for (String log : logs) {
                consoleOutput.append(log + "\n");
                System.out.println("Hello, world!");
            }
            consoleOutput.setCaretPosition(consoleOutput.getDocument().getLength());
        }
    }
}




class TrainView extends JPanel implements Observer {
    private BufferedImage banditImg;
    private BufferedImage bourseImg;
    private BufferedImage magotImg;
    private BufferedImage bijouxImg;
    private BufferedImage marshallImg;
    private BufferedImage wagonImg;


    private ColtModel model;
    private final static int TAILLE = 200;
    private ImageIcon backgroundImage;

    public TrainView(ColtModel model) {

        this.model = model;

        model.addObserver(this);

        Dimension dim = new Dimension(TAILLE * ColtModel.NB_WAGONS, TAILLE * ColtModel.NB_WAGONS);
        this.setPreferredSize(dim);
        backgroundImage = new ImageIcon("media/background.gif");

        File wheel = new File("media/wheel.jpg");
        File banditF = new File("media/bandit1.jpg");
        File bijoux = new File("media/bijoux.jpg");
        File magot = new File("media/treasure.png");
        File bourse = new File("media/bourses.jpg");
        File marshallF = new File("media/sheriff.jpg");
        File wagon = new File("media/wagon.jpg");


        try {
            banditImg = ImageIO.read(banditF);
            bourseImg = ImageIO.read(bourse);
            magotImg = ImageIO.read(magot);
            bijouxImg = ImageIO.read(bijoux);
            marshallImg = ImageIO.read(marshallF);
            wagonImg = ImageIO.read(wagon);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Observable o, Object arg) {
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        backgroundImage.paintIcon(this,g,0,0);
        drawTrain(g);
    }

    private void drawTrain(Graphics g) {

        TrainElement[] trainElements = model.getTrainLayout();
        Marshall marshall = model.getMarshall();

        if (trainElements != null) {

            int x = 0;

            for (TrainElement element : trainElements) {

                g.drawImage(wagonImg,x,  600 - TAILLE*2/3, TAILLE , TAILLE * 2/3,this);

                int banditX = 0;

                for (Bandit bandit : element.getCharacters()) {
                    int y = bandit.onTheRoof()?0:1;
                    g.drawImage(banditImg,banditX+(bandit.getIndex()*TAILLE) +16, -(TAILLE/4) + 600 - TAILLE*2/3 +5 +100*y, TAILLE/4, TAILLE/4, this);
                    g.drawString(bandit.getName(),banditX+(bandit.getIndex()*TAILLE) + 7 +16,-(TAILLE/4) + 600 - TAILLE*2/3 +5 +100*y -10);
                    banditX += TAILLE/6;
                }

                g.drawImage(marshallImg,marshall.getIndex()*TAILLE + 16, -(TAILLE/4) + 600 - TAILLE*2/3 +5 +100, TAILLE/4, TAILLE/4, this);

                ArrayList<LootElement> butins = element.getLoot();


                int lootX = 16 ;
                for(LootElement butin: butins){
                    if(butin instanceof Bourse){
                        g.drawImage(bourseImg,x + lootX ,TAILLE/2 + 600 - TAILLE*2/3 - 20,TAILLE/8,TAILLE/8,this);
                    }else if(butin instanceof Bijoux){
                        g.drawImage(bijouxImg,x + lootX ,TAILLE/2 + 600 - TAILLE*2/3 - 20,TAILLE/8,TAILLE/8,this);
                    }else{
                        g.drawImage(magotImg,x + lootX,TAILLE/2 + 600 - TAILLE*2/3 - 20,TAILLE/8,TAILLE/8,this);
                    }
                    lootX += TAILLE/5;
                }

                x += TAILLE;
            }
        }
    }
}

class Commandsview extends JPanel implements Observer {
    private final ColtModel model;
    private boolean foo;

    private final JButton nextstep, avance, grimpe, recule, descend,  braque,reset;
    private final JComboBox tire;
    private final JMenuBar menuBar;

    public Commandsview(ColtModel model) {

        this.model = model;
        model.addObserver(this);

        ColtController controller = new ColtController(model);

        menuBar = new JMenuBar();
        avance = new JButton("Avance");
        grimpe = new JButton("Grimpe");
        recule = new JButton("Recule");
        descend = new JButton("Descend");
        String[] directions = {"Devant", "Derriere", "Haut", "Bas"};

        tire = new JComboBox<>(directions);

        tire.addActionListener(e -> {
            String selectedDirection = (String) tire.getSelectedItem();
            tire.setActionCommand(selectedDirection);
        });

        braque = new JButton("Braque");

        avance.setActionCommand("Avance");
        avance.addActionListener(controller);

        grimpe.setActionCommand("Grimpe");
        grimpe.addActionListener(controller);

        recule.setActionCommand("Recule");
        recule.addActionListener(controller);

        descend.setActionCommand("Descend");
        descend.addActionListener(controller);

        tire.setActionCommand("Tire");
        tire.addActionListener(controller);

        braque.setActionCommand("Braque");
        braque.addActionListener(controller);

        this.add(avance);
        this.add(grimpe);
        this.add(recule);
        this.add(descend);
        this.add(tire);
        this.add(braque);

        nextstep = new JButton("Prochain Joueur");
        reset = new JButton("Reset");
        this.add(nextstep);
        this.add(menuBar);
        this.add(reset);

        reset.addActionListener(controller);
        reset.setEnabled(false);

        nextstep.addActionListener(controller);
        nextstep.setEnabled(false);
    }


    @Override
    public void update(Observable o, Object arg) {

        if (model.getCurrentplayer().getActions().size() < 4) {

            if(! model.getCurrentplayer().getActions().isEmpty()){
                reset.setEnabled(true);
            }else{
                reset.setEnabled(false);
            }

            if (nextstep.getText() == "Action !") {
                nextstep.setText("Prochain Joueur");
                nextstep.setActionCommand("Prochain Joueur");
            }

            foo = false;
        }
        else
        {
            if (model.getTurnnumber() == ColtModel.NB_PLAYERS -1 ){
                nextstep.setText("Action !");
                nextstep.setActionCommand("Action");
            }
            foo = true;
        }



        nextstep.setEnabled(foo);
        avance.setEnabled(!foo);
        grimpe.setEnabled(!foo);
        recule.setEnabled(!foo);
        descend.setEnabled(!foo);
        tire.setEnabled(!foo);
        braque.setEnabled(!foo);
    }
}


class ScoreBoardView extends JPanel implements Observer {
    private JPanel scorePanel ;
    private ColtModel model;
    private BanditStats [] bandits;

    public  ScoreBoardView(ColtModel model) {

        this.model = model;
        model.addObserver(this);


        Bandit[] banditlist = model.getBandits();

        scorePanel = new JPanel(new FlowLayout());

        bandits = new BanditStats[banditlist.length];

        for (int i = 0 ; i <banditlist.length;i++){
            bandits[i] = new BanditStats(banditlist[i]);
            model.addObserver(bandits[i]);
            scorePanel.add(bandits[i]);
        }
        add(scorePanel);

    }

    public void update(Observable o, Object arg) {
        for (BanditStats stat : bandits) {
            stat.update(o,null);
            stat.setBackground(Color.WHITE);

            if (Objects.equals(model.getCurrentplayer().name, stat.getBandit().name)) {
                stat.setBackground(Color.green);
            }

        }
    }

}


class BanditStats extends JPanel implements Observer  {
    private Bandit bandit;
    private JLabel scoreLabel;
    private JLabel mugshotLabel;
    private JLabel nameLabel;
    private JLabel numbullets;

    public BanditStats (Bandit bandit) {
        this.bandit = bandit;
        setLayout(new GridBagLayout());


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.insets = new Insets(5, 5, 5, 5);

        nameLabel = new JLabel(bandit.getName().toUpperCase());
        add(nameLabel, gbc);

        gbc.gridy++;
        scoreLabel = new JLabel("Score: " + bandit.getScore());
        add(scoreLabel, gbc);

        gbc.gridy++;
        numbullets = new JLabel("Bullets: " + bandit.getRemaining_bullets());
        add(numbullets, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(5, 20, 5, 5);

        mugshotLabel = new JLabel();
        setImage("media/mugshot.jpg"); // Set the image for the mugshot
        mugshotLabel.setPreferredSize(new Dimension(100, 100));
        add(mugshotLabel, gbc);

    }

    public void setImage(String imagePath) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            Image scaledImage = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaledImage);
            mugshotLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mugshotLabel.setVerticalAlignment(SwingConstants.CENTER);
            mugshotLabel.setIcon(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateBandit () {
        scoreLabel.setText("Score: " + bandit.getScore());
        numbullets.setText("Bullets :"+bandit.getRemaining_bullets());
    }

    public Bandit getBandit () {
        return bandit;
    }

    @Override
    public void update(Observable o, Object arg) {
        updateBandit();
    }
}

class StartScreen extends JPanel {
    private final JFrame frame;


    public StartScreen() {
        frame = new JFrame();
        frame.setTitle("Colt Express - Start Screen");
        frame.setPreferredSize(new Dimension(800, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel titleLabel = new JLabel("Welcome to Colt Express");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JLabel playerCountLabel = new JLabel("Select number of players:");
        JComboBox<Integer> playerCountDropdown = new JComboBox<>(new Integer[]{2, 3, 4, 5, 6});
        playerCountDropdown.setSelectedIndex(0);

        JButton startButton = new JButton("Start Game");
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int playerCount = (Integer) playerCountDropdown.getSelectedItem();
                getPlayerNames(playerCount);
            }
        });

        setLayout(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout());
        titlePanel.add(titleLabel);
        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        centerPanel.add(playerCountLabel);
        centerPanel.add(playerCountDropdown);
        centerPanel.add(startButton);
        add(titlePanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void getPlayerNames(int playerCount) {
        String[] playerNames = new String[playerCount];
        for (int i = 0; i < playerCount; i++) {
            String name = JOptionPane.showInputDialog(frame, "Enter player " + (i + 1) + "'s name:","El Matador");
            if (name == null || name.isEmpty()) {
                name = "Player " + (i + 1);
            }
            playerNames[i] = name;
        }
        frame.dispose();
        startGame(playerNames);
    }

    private void startGame(String[] playerNames) {
        EventQueue.invokeLater(() -> {
            ColtModel model = new ColtModel(playerNames);
            ColtView view = new ColtView(model);
        });
    }
}




public class ColtExpress {

    public static void main(String[] args) {

        EventQueue.invokeLater(StartScreen::new);
    }
}

/////////////////////////////////////////////////////////////////



//////////CONTROLLER////////////////////////////////////////////
class ColtController implements ActionListener {
    private final ColtModel model;
    private int i =0;
    public ColtController(ColtModel model) {this.model = model;}
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        if (e.getActionCommand().equals("Action")){
                boolean done = i==3;
                model.executeBanditAction(i,done);
                if(done)
                {
                    i=0;
                }else{
                    i++;
                }
        }

        else if (e.getActionCommand().equals("Prochain Joueur")) {
            model.nextplayer();
        }else if(e.getActionCommand().equals("Reset")){
            model.clearplayerselection();
        }
        else {

            model.addBanditAction(Action.convertToAction(actionCommand));
        }

    }


}
/////////////////////////////////////////////////////////////////
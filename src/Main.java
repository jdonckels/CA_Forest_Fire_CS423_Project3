/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//TODO implement GA instead of iterating over p values (fitness = biomass*longevity maybe?)
//TODO 2 treeSpecies

/**
 * This is my main class that contains the game loop and the majority of the calculations.
 * This creates a screen and attaches everything to it
 *
 * @author joshu
 */
public class Main extends Application implements EventHandler<KeyEvent>
{
  private Random random = new Random();
  private final Group root = new Group();

  private final PerspectiveCamera camera = new PerspectiveCamera(true);
  private final Xform cameraXform = new Xform();
  private final Xform cameraXform2 = new Xform();
  private final Xform cameraXform3 = new Xform();
  private final double CAMERA_INITIAL_DISTANCE = -500;
  private double cameraDistance = -500;
  private final double CAMERA_INITIAL_X_ANGLE = 0.0;
  private final double CAMERA_INITIAL_Y_ANGLE = 0.0;
  private final double CAMERA_NEAR_CLIP = 0.1;
  private final double CAMERA_FAR_CLIP = 10000.0;
  private final double LIGHTNING_STRIKE_PROB = 0.001;

  private double p[] = new double[100];
  private double p_two[] = new double[100];
  private double bio[] = new double[100];
  private double bio_two[] = new double[100];
  private double longev[] = new double[100];
  private double longev_two[] = new double[100];
  private double fitness[] = new double[100];
  private double fitness_two[] = new double[100];
  private double top_fitness[] = new double[100];
  private double top_fitness_two[] = new double[100];
  private double top_Pval[] = new double[100];
  private double top_Pval_two[] = new double[100];

  //All the values that one can change too affect the model, and what it output to cvs, or to the GUI
  private boolean twoSpecies = false;
  private final boolean SLOW = true;
  private final boolean GUI = true;
  private final boolean DEBUG = false;
  private final boolean FIREFIGHTERS = false;
  private final boolean GA = true;
  private final int MAX_STEPS = 5000;
  private int numberOfFireFighters = 0;
  private int iteration = 0;

  private static Cell[][] graph = new Cell[252][252]; //252 for padding
  private int[][] graphNoGUI = new int[252][252]; //252 for graph without GUI
  private int[][] nextState = new int[252][252]; //252 to match the graphs size, makes the logic simpler to understand.

  private Loop loop = new Loop();

  /**
   * This is run before anything else to create the window and attach all the scenes and everything needed
   * to the window.
   *
   * @param primaryStage
   */
  @Override
  public void start(Stage primaryStage)
  {
    if(!GA)
    {
      for (int i = 0; i < 100; i++)
      {
        p[i] = (i + 1) * 0.01;
      }
    }

    //If GUI is set run the GUI
    if(GUI)
    {
      BorderPane pane = new BorderPane();
      Scene topScene = new Scene(pane, 768, 768, true, SceneAntialiasing.BALANCED);
      topScene.setOnKeyPressed(this);

      SubScene scene = new SubScene(root, 768, 768, true, SceneAntialiasing.BALANCED);
      scene.setFill(Color.BLACK);

      pane.setCenter(scene);

      scene.widthProperty().bind(pane.widthProperty());

      buildCamera();
      buildBoard(true);

      primaryStage.setTitle("Forest Fire");
      primaryStage.setScene(topScene);
      primaryStage.show();

      scene.setCamera(camera);

      loop.start();
      loop.running = true;
    }
    //If GA is set run no graphic
    else
    {
      buildBoard(true);
      loop.start();
      loop.running = true;
    }
  }

  /**
   * Initializes the board, for GUI or nonGUI
   * @param first_iteration if this is the first iteration or not
   */
  private void buildBoard(boolean first_iteration)
  {
    if(GUI)
    {
      for(int i = 0; i <= 251; i++)
      {
        for(int j = 0; j <= 251; j++)
        {
          nextState[i][j] = 0;
          graph[i][j] = new Cell(i, j, 0);
          graph[i][j].getCell().setTranslateX(i - 125);
          graph[i][j].getCell().setTranslateY(j - 125);
          root.getChildren().add(graph[i][j].getCell());
        }
      }

      if(first_iteration)
      {
        for (int i = 1; i <= 250; i++)
        {
          for (int j = 1; j <= 250; j++)
          {
            graph[i][j].setNeighbors(getNeighbors(i, j));
          }
        }
      }
    }
    else
    {
      for(int i = 0; i <= 251; i++)
      {
        for(int j = 0; j <= 251; j++)
        {
          nextState[i][j] = 0;
          graphNoGUI[i][j] = 0;
        }
      }
    }
  }

  /**
   * This is only for the GUI, and will allow for performance increases, because then it does not need
   * to recalculate the neighbors every iteration.
   * @param i
   * @param j
   * @return list of neighbors
   */
  private List<Cell> getNeighbors(int i, int j)
  {
    List<Cell> neighbors = new ArrayList<>();

    for(int row = i - 1; row <= i + 1; row++)
    {
      for(int col = j - 1; col <= j + 1; col++)
      {
        if(!(row == i && col == j))
        {
          if(row > 0 && col > 0 && row < 251 && col < 251)
          {
            neighbors.add(graph[row][col]);
          }
        }
      }
    }
    return neighbors;
  }

  /**
   * This overrides the handle for the event handler listening to the keyboard. It is ran everytime a key
   * is pressed by the user.
   *
   * @param e the input key
   */
  @Override
  public void handle(KeyEvent e)
  {
    if(e.getCode() == KeyCode.O)
    {
      cameraDistance -= 2;
      camera.setTranslateZ(cameraDistance);
    }
    else if(e.getCode() == KeyCode.I)
    {
      cameraDistance += 2;
      camera.setTranslateZ(cameraDistance);
    }
    else if(e.getCode() == KeyCode.P)
    {
      if(loop.running)
      {
        loop.stop();
        loop.running = false;
      }
      else
      {
        loop.start();
        loop.running = true;
      }
    }
  }

  /**
   * This builds the camera and adds it to an xform so it can be rotate and/or moved easily.
   */
  private void buildCamera()
  {
    root.getChildren().add(cameraXform);
    cameraXform.getChildren().add(cameraXform2);
    cameraXform2.getChildren().add(cameraXform3);
    cameraXform3.getChildren().add(camera);
    cameraXform3.setRotateZ(180.0);

    camera.setNearClip(CAMERA_NEAR_CLIP);
    camera.setFarClip(CAMERA_FAR_CLIP);
    camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
    cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
    cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
    cameraXform.ry.setPivotX(0);
  }

  /**
   * This deals with saving the results into a cvs file, and parsing the results accordingly
   */
  private void saveResults()
  {
    BufferedWriter bw = null;
    FileWriter fw = null;

    try
    {
      String filename = "";
      if(!FIREFIGHTERS)
      {
        filename = "./data/ForestFireData.csv";
        fw = new FileWriter(filename,false);
        bw = new BufferedWriter(fw);
        if(!GA)
        {
          bw.write("pVal, biomass, longevity\n");
          for (int i = 0; i < 100; i++)
          {
            bw.write(p[i] + ", " + bio[i] + ", " + longev[i] + "\n");
          }
        }
        else
        {
          if(!twoSpecies)
          {
            bw.write("pVal, biomass, longevity, fitness, top_pval, top_fitness\n");
            for (int i = 0; i < 100; i++)
            {
              bw.write(p[i] + ", " + bio[i] + ", " + longev[i] + ", " + fitness[i] + ", " + top_Pval[i] + ", " + top_fitness[i] + "\n");
            }
          }
          else
          {
            bw.write("pVal, biomass, longevity, fitness, top_pval, top_fitness, pVal_Two, biomass_Two, longevity_Two, top_Fitness_Two, top_Pval_Two\n");
            for (int i = 0; i < 100; i++)
            {
              bw.write(p[i] + ", " + bio[i] + ", " + longev[i] + ", " + fitness[i] + ", " + top_Pval[i] + ", " + top_fitness[i] + ", " + p_two[i]
                       + ", " + bio_two[i] + ", " + longev_two[i] + ", " + fitness_two[i] + ", " + top_Pval_two[i] + ", " + top_fitness_two[i] +"\n");
            }
          }
        }
      }
      else
      {
        filename =  "./data/ForestFireDataWithFireFighters.csv";
        fw = new FileWriter(filename,false);
        bw = new BufferedWriter(fw);
        bw.write("(0.5 prob tree) firefighters, biomass, longevity \n");
        for(int i = 0; i <= 20; i++)
        {
          bw.write( (i * 50)+ ", " + bio[i] + ", " + longev[i] + "\n");
        }
      }

    } catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      try {

        if (bw != null)
          bw.close();

        if (fw != null)
          fw.close();

      } catch (IOException ex) {

        ex.printStackTrace();

      }
    }
  }

  /**
   * This is the main game loop that is ran based on when the user clicking the run button.
   */
  class Loop extends AnimationTimer
  {
    boolean running = false;
    int frame = 0;
    private TreeSpecies one = new TreeSpecies(0.1, Color.FORESTGREEN);
    private TreeSpecies two = new TreeSpecies(0.05, Color.DARKOLIVEGREEN);
    private TreeSpecies top_tree = one;
    private TreeSpecies top_tree_two = two;
    private boolean first_iteration = true;
    private int numberOfFireFightersLeft = 0;
    private List<Double> unused_p_values = new ArrayList<>();
    private List<Double> unused_p_values_two = new ArrayList<>();
    private boolean fill_p_values = true;


    @Override
    public void handle(long time)
    {
      if(GA)
      {
        if(fill_p_values)
        {
          for(int i = 0; i < 100; i++)
          {
            unused_p_values.add((i + 1) * 0.01);
          }
          fill_p_values = false;
          unused_p_values.remove(one.getProbability());
          for(int i = 0; i < 100; i++)
          {
            unused_p_values_two.add((i + 1) * 0.01);
          }
          unused_p_values_two.remove(two.getProbability());
        }

        if(unused_p_values.size() > 10 && unused_p_values_two.size() > 10)
        {
          if (frame == 0 && !first_iteration)
          {
            double random_value = getRandomValue(false);
            int stuck = 0;
            while(!unused_p_values.contains(random_value))
            {
              if(stuck <= 10)
              {
                random_value = getRandomValue(false);
              }
              else
              {
                BigDecimal b = new BigDecimal(random.nextDouble());
                b = b.setScale(2, RoundingMode.CEILING);
                random_value = b.doubleValue();
              }
              stuck++;
            }
            unused_p_values.remove(random_value);
            if(DEBUG) System.out.println("iteration: " + iteration + "  one: " + random_value);
            p[iteration] = random_value;
            one = new TreeSpecies(random_value, Color.FORESTGREEN);
            if(twoSpecies)
            {
              double random_value_two = getRandomValue(true);
              int stuck_two = 0;
              while(!unused_p_values_two.contains(random_value_two))
              {
                if(stuck_two <= 10)
                {
                  random_value_two = getRandomValue(true);
                }
                else
                {
                  BigDecimal b = new BigDecimal(random.nextDouble());
                  b = b.setScale(2, RoundingMode.CEILING);
                  random_value_two = b.doubleValue();
                }
                stuck_two++;
              }
              unused_p_values_two.remove(random_value_two);
              if(DEBUG) System.out.println("iteration: " + iteration + "  two: " + random_value_two);
              p_two[iteration] = random_value_two;
              two = new TreeSpecies(random_value_two, Color.DARKOLIVEGREEN);
            }
          }

          if(first_iteration)
          {
            first_iteration = false;
          }

          if (frame < MAX_STEPS)
          {
            if((frame % 25 == 0 && SLOW) || !SLOW)
            {
              if (GUI)
              {
                updateGraph();
              }
              else
              {
                updateGraphNoGraphic();
              }
            }
            frame++;
          }
          else
          {
            one.setBiomass(one.getBiomass() / MAX_STEPS);

            bio[iteration] = one.getBiomass();
            if (one.getLongevity() != 0)
            {
              longev[iteration] = one.getLongevity();
              one.setFitness(one.getLongevity() * one.getBiomass());
            }
            else
            {
              longev[iteration] = MAX_STEPS;
              one.setFitness(5000 * one.getBiomass());
            }

            if(top_tree == null || top_tree.getFitness() < one.getFitness()) top_tree = one;
            if(DEBUG) System.out.println("top:" + top_tree.getFitness() + " vs one:" + one.getFitness());
            top_fitness[iteration] = top_tree.getFitness();
            fitness[iteration] = one.getFitness();
            top_Pval[iteration] = top_tree.getProbability();

            if(twoSpecies)
            {
              two.setBiomass(two.getBiomass() / MAX_STEPS);

              bio_two[iteration] = two.getBiomass();
              if (two.getLongevity() != 0)
              {
                longev_two[iteration] = two.getLongevity();
                two.setFitness(two.getLongevity() * two.getBiomass());
              }
              else
              {
                longev_two[iteration] = MAX_STEPS;
                two.setFitness(5000 * two.getBiomass());
              }

              if(top_tree_two == null || top_tree_two.getFitness() < two.getFitness()) top_tree_two = two;
              if(DEBUG) System.out.println("top:" + top_tree_two.getFitness() + " vs two:" + two.getFitness());
              top_fitness_two[iteration] = top_tree_two.getFitness();
              fitness_two[iteration] = two.getFitness();
              top_Pval_two[iteration] = top_tree_two.getProbability();

              two.setBiomass(0.0);
              two.setLongevity(0.0);
            }

            iteration++;
            frame = 0;
            one.setBiomass(0.0);
            one.setLongevity(0.0);
            buildBoard(false);
          }
        }
        else
        {
          saveResults();
          stop();
          if (!GUI)
          {
            Platform.exit();
          }
        }

      }
      else
      {
        if (iteration < 100 && numberOfFireFighters <= 1000)
        {
          if (frame == 0 && !FIREFIGHTERS)
          {
            if(DEBUG) System.out.println("iteration: " + iteration);
            one = new TreeSpecies(p[iteration], Color.FORESTGREEN);
          }
          if (frame < MAX_STEPS)
          {
            numberOfFireFightersLeft = numberOfFireFighters;
            if((frame % 25 == 0 && SLOW) || !SLOW)
            {
              if (GUI)
              {
                updateGraph();
              }
              else
              {
                updateGraphNoGraphic();
              }
            }
            frame++;
            if (DEBUG) System.out.println("frame:" + frame);
          }
          else
          {
            one.setBiomass(one.getBiomass() / MAX_STEPS);
            if (DEBUG) System.out.println("One biomass: " + one.getBiomass() * 100 + "%");
            bio[iteration] = one.getBiomass();
            if (one.getLongevity() != 0)
            {
              longev[iteration] = one.getLongevity();
            }
            else
            {
              longev[iteration] = MAX_STEPS;
            }
            iteration++;
            frame = 0;
            one.setBiomass(0.0);
            one.setLongevity(0.0);
            if (FIREFIGHTERS) numberOfFireFighters += 50;
            buildBoard(false);
          }
        }
        else
        {
          saveResults();
          stop();
          if (!GUI)
          {
            Platform.exit();
          }
        }
      }
    }

    /**
     * Gets a random value within a range of +- 0.05 from the current p-value.
     * @param tree_two if this is meant for tree one or two
     * @return returns the new p-value
     */
    private double getRandomValue(boolean tree_two)
    {
      double rangeMin, rangeMax, prob;
      if(!tree_two) prob = top_tree.getProbability();
      else prob = top_tree_two.getProbability();
      if(prob >= 0.05)
      {
        rangeMin = prob - 0.05;
      }
      else
      {
        rangeMin = 0;
      }

      if(prob <= 0.95)
      {
        rangeMax = prob + 0.05;
      }
      else
      {
        rangeMax = 1;
      }

      BigDecimal randomValue = new BigDecimal(rangeMin + (rangeMax - rangeMin) * random.nextDouble());
      randomValue = randomValue.setScale(2, RoundingMode.CEILING);
      return randomValue.doubleValue();
    }

    /**
     * Call what is needed to update the graph, this is for GUI, and also sets biomasses, and longevity's.
     */
    private void updateGraph()
    {
      double biomass[] = setCellNextState();

      one.setBiomass(one.getBiomass() + (biomass[0] / 62500.0));
      if(biomass[0] == 0 && frame > 0)
      {
        if(one.getLongevity() == 0)
        {
          one.setLongevity(frame);
        }
      }

      if(twoSpecies)
      {
        two.setBiomass(two.getBiomass() + (biomass[1] / 62500.0));
        if(biomass[1] == 0 && frame > 0)
        {
          if(two.getLongevity() == 0)
          {
            two.setLongevity(frame);
          }
        }
      }

      updateCells();
    }

    /**
     * Call what is needed to update the graph, this is for no-GUI, and also sets biomasses, and longevity's.
     */
    private void updateGraphNoGraphic()
    {
      double biomass[] = setCellNextStateNoGUI();

      one.setBiomass(one.getBiomass() + (biomass[0] / 62500.0));
      if(biomass[0] == 0 && frame > 0)
      {
        if(one.getLongevity() == 0)
        {
          one.setLongevity(frame);
        }
      }

      if(twoSpecies)
      {
        two.setBiomass(two.getBiomass() + (biomass[1] / 62500.0));
        if(biomass[1] == 0 && frame > 0)
        {
          if(two.getLongevity() == 0)
          {
            two.setLongevity(frame);
          }
        }
      }

      updateCellsNoGUI();
    }

    /**
     * Updates the state based on the status, for GUI
     * @return biomasses for tree one and two if needed
     */
    private double[] setCellNextState()
    {
      double biomass[] = {0.0, 0.0};

      for(int i = 1; i <= 250; i++)
      {
        for(int j = 1; j <= 250; j++)
        {
          int status = graph[i][j].getStatus();
          if(status == 4)
          {
            nextState[i][j] = 1;
          }
          else if(status == 3)
          {
            //On fire, go to barren and set all neighbor trees on fire
            nextState[i][j] = 0;
            for (Cell c : graph[i][j].getNeighbors())
            {
              if (c.getStatus() == 1 || c.getStatus() == 2)
              {
                nextState[c.getX()][c.getY()] = 3;
              }
            }
          }
          //if there is a tree, check for lightning strike
          else if(status == 1 || status == 2)
          {
            if(status == 1) biomass[0] += 1.0;
            else if(status == 2) biomass[1] += 1.0;
            if(random.nextDouble() < LIGHTNING_STRIKE_PROB)
            {
              nextState[i][j] = 3;
            }
          }
          //if barren chance to spawn tree
          else if(status == 0)
          {
            if (twoSpecies)
            {
              if(one.getProbability() >= two.getProbability())
              {
                if(random.nextDouble() < one.getProbability())
                {
                  nextState[i][j] = 1;
                }
                else if(random.nextDouble() < two.getProbability())
                {
                  nextState[i][j] = 2;
                }
              }
              else
              {
                if(random.nextDouble() < two.getProbability())
                {
                  nextState[i][j] = 2;
                }
                else if(random.nextDouble() < one.getProbability())
                {
                  nextState[i][j] = 1;
                }
              }
            }
            else
            {
              if (random.nextDouble() < one.getProbability())
              {
                nextState[i][j] = 1;
              }
            }
          }
        }
      }

      return biomass;
    }

    /**
     * Updated the corresponding cells to their next state, for GUI
     */
    private void updateCells()
    {
      for(int i = 1; i <= 250; i++)
      {
        for(int j = 1; j <= 250; j++)
        {
          int status = nextState[i][j];
          if(graph[i][j].getStatus() != status)
          {
            if(status == 3)
            {
              if(FIREFIGHTERS && numberOfFireFightersLeft > 0)
              {
                graph[i][j].getCell().setFill(Color.BLUE);
                graph[i][j].getCell().setVisible(true);
                graph[i][j].setStatus(4);
                numberOfFireFightersLeft--;
              }
              else
              {
                graph[i][j].getCell().setFill(Color.ORANGE);
                graph[i][j].getCell().setVisible(true);
                graph[i][j].setStatus(3);
              }
            }
            else if(status == 1)
            {
              graph[i][j].getCell().setFill(one.getColor());
              graph[i][j].getCell().setVisible(true);
              graph[i][j].setStatus(1);
            }
            else if(status == 2)
            {
              graph[i][j].getCell().setFill(two.getColor());
              graph[i][j].getCell().setVisible(true);
              graph[i][j].setStatus(2);
            }
            else if(status == 0)
            {
              graph[i][j].getCell().setVisible(false);
              graph[i][j].setStatus(0);
            }
          }
        }
      }
    }

    /**
     * Updates the state based on the status, for non GUI
     * @return biomasses for tree one and two if needed
     */
    private double[] setCellNextStateNoGUI()
    {
      double biomass[] = {0.0, 0.0};

      for (int i = 1; i <= 250; i++)
      {
        for (int j = 1; j <= 250; j++)
        {
          int status = graphNoGUI[i][j];
          if(status == 4)
          {
            nextState[i][j] = 1;
          }
          else if (status == 3)
          {
            //On fire, go to barren and set all neighbor trees on fire
            nextState[i][j] = 0;
            for (int row = i - 1; row <= i + 1; row++)
            {
              for (int col = j - 1; col <= j + 1; col++)
              {
                if (!(row == i && col == j))
                {
                  if (row > 0 && col > 0 && row < 251 && col < 251)
                  {
                    if (graphNoGUI[row][col] == 1 || graphNoGUI[row][col] == 2)
                    {
                      nextState[row][col] = 3;
                    }
                  }
                }
              }
            }
          }
          //if there is a tree, check for lightning strike
          else if(status == 1 || status == 2)
          {
            if(status == 1) biomass[0] += 1.0;
            else if(status == 2) biomass[1] += 1.0;
            if(random.nextDouble() < LIGHTNING_STRIKE_PROB)
            {
              nextState[i][j] = 3;
            }
          }
          //if barren chance to spawn tree
          else if(status == 0)
          {
            if (twoSpecies)
            {
              if(one.getProbability() >= two.getProbability())
              {
                if(random.nextDouble() < one.getProbability())
                {
                  nextState[i][j] = 1;
                }
                else if(random.nextDouble() < two.getProbability())
                {
                  nextState[i][j] = 2;
                }
              }
              else
              {
                if(random.nextDouble() < two.getProbability())
                {
                  nextState[i][j] = 2;
                }
                else if(random.nextDouble() < one.getProbability())
                {
                  nextState[i][j] = 1;
                }
              }
            }
            else
            {
              if (random.nextDouble() < one.getProbability())
              {
                nextState[i][j] = 1;
              }
            }
          }
        }
      }

      return biomass;
    }

    /**
     * Updated the corresponding cells to their next state, for non-GUI
     */
    private void updateCellsNoGUI()
    {
      for(int i = 1; i <= 250; i++)
      {
        for(int j = 1; j <= 250; j++)
        {
          int status = nextState[i][j];
          if(graphNoGUI[i][j] != status)
          {
            if(status == 3)
            {
              if(FIREFIGHTERS && numberOfFireFightersLeft > 0)
              {
                graphNoGUI[i][j] = 4;
                numberOfFireFightersLeft--;
              }
              else
              {
                graphNoGUI[i][j] = 3;
              }
            }
            else if(status == 1)
            {
              graphNoGUI[i][j] = 1;
            }
            else if(status == 2)
            {
              graphNoGUI[i][j] = 2;
            }
            else if(status == 0)
            {
              graphNoGUI[i][j] = 0;
            }
          }
        }
      }
    }
  }


  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {
    launch(args);
  }

}

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
  private double bio[] = new double[100];
  private double longev[] = new double[100];

  private boolean twoSpecies = false;
  private final boolean GUI = false;
  private final boolean DEBUG = false;
  private final boolean FIREFIGHTERS = true;
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
    for(int i = 0; i < 100; i++)
    {
      p[i] = (i + 1) * 0.01;
    }

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
      buildBoard();

      primaryStage.setTitle("Forest Fire");
      primaryStage.setScene(topScene);
      primaryStage.show();

      scene.setCamera(camera);

      loop.start();
      loop.running = true;
    }
    else
    {
      buildBoard();
      loop.start();
      loop.running = true;
    }
  }

  /**
   * This function is used to build the initial board, so the only thing that needs to be called
   * for every cell is toLife() or toDeath().
   */
  private void buildBoard()
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

      for(int i = 1; i <= 250; i++)
      {
        for(int j = 1; j <= 250; j++)
        {
          graph[i][j].setNeighbors(getNeighbors(i, j));
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
        bw.write("pVal, biomass, longevity\n");
        for(int i = 0; i < 100; i++)
        {
          bw.write(p[i] + ", " + bio[i] + ", " + longev[i] + "\n");
        }
      }
      else
      {
        filename =  "./data/ForestFireDataWithFireFighters.csv";
        fw = new FileWriter(filename,false);
        bw = new BufferedWriter(fw);
        bw.write("pVal, firefighters, biomass, longevity \n");
        for(int i = 0; i < 100; i++)
        {
          bw.write(p[i] + ", " + numberOfFireFighters + ", " + bio[i] + ", " + longev[i] + "\n");
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
    private TreeSpecies one = new TreeSpecies(0.5, Color.FORESTGREEN);
    private TreeSpecies two = new TreeSpecies(0.5, Color.DARKOLIVEGREEN);
    private int numberOfFireFightersLeft = 0;


    @Override
    public void handle(long time)
    {
      if(iteration < 100 && numberOfFireFighters <= 1000)
      {
        if(frame == 0 && !FIREFIGHTERS)
        {
          System.out.println("iteration: " + iteration);
          one = new TreeSpecies(p[iteration], Color.FORESTGREEN);
        }
        if(frame < MAX_STEPS)
        {
          numberOfFireFightersLeft = numberOfFireFighters;
          if(GUI)
          {
            updateGraph();
          }
          else
          {
            updateGraphNoGraphic();
          }
          frame++;
          if(DEBUG) System.out.println("frame:" + frame);
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
          numberOfFireFighters += 50;
        }
      }
      else
      {
        saveResults();
        stop();
        if(!GUI)
        {
          Platform.exit();
        }
      }
    }

    private void updateGraph()
    {
      double biomass = setCellNextState();

      biomass /= 62500.0;
      one.setBiomass(one.getBiomass() + biomass);
      if(biomass == 0 && frame > 0)
      {
        if(one.getLongevity() == 0)
        {
          one.setLongevity(frame);
        }
      }

      updateCells();
    }

    private void updateGraphNoGraphic()
    {
      double biomass = setCellNextStateNoGUI();

      biomass /= 62500.0;
      one.setBiomass(one.getBiomass() + biomass);
      if(biomass == 0 && frame > 0)
      {
        if(one.getLongevity() == 0)
        {
          one.setLongevity(frame);
        }
      }

      updateCellsNoGUI();
    }

    //Updates the state based on the status
    private double setCellNextState()
    {
      double biomass = 0.0;

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
              if (c.getStatus() == 1 || (status == 2 && twoSpecies))
              {
                nextState[c.getX()][c.getY()] = 3;
              }
            }
          }
          //if there is a tree, check for lightning strike
          else if(status == 1 || (status == 2 && twoSpecies))
          {
            biomass += 1.0;
            if(random.nextDouble() < LIGHTNING_STRIKE_PROB)
            {
              nextState[i][j] = 3;
            }
          }
          //if barren chance to spawn tree
          else if(status == 0)
          {
            if(random.nextDouble() < one.getProbability())
            {
              nextState[i][j] = 1;
            }
            else if(twoSpecies)
            {
              if(random.nextDouble() < two.getProbability())
              {
                nextState[i][j] = 2;
              }
            }
          }
        }
      }

      return biomass;
    }

    //Updates cell only when needed
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
            else if(status == 2 && twoSpecies)
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

    //Updates the state based on the status
    private double setCellNextStateNoGUI()
    {
      double biomass = 0.0;

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
                    if (graphNoGUI[row][col] == 1 || (status == 2 && twoSpecies))
                    {
                      nextState[row][col] = 3;
                    }
                  }
                }
              }
            }
          }
          //if there is a tree, check for lightning strike
          else if (status == 1 || (status == 2 && twoSpecies))
          {
            biomass += 1.0;
            if (random.nextDouble() < LIGHTNING_STRIKE_PROB)
            {
              nextState[i][j] = 3;
            }
          }
          //if barren chance to spawn tree
          else if (status == 0)
          {
            if (random.nextDouble() < one.getProbability())
            {
              nextState[i][j] = 1;
            }
            else if (twoSpecies)
            {
              if (random.nextDouble() < two.getProbability())
              {
                nextState[i][j] = 2;
              }
            }
          }
        }
      }

      return biomass;
    }

    //Updates cell only when needed
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
            else if(status == 2 && twoSpecies)
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

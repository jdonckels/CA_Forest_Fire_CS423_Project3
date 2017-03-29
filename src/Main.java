/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This is my main class that contains the game loop and the majority of the calculations.
 * This creates a screen and attaches everything to it
 *
 * @author joshu
 */
public class Main extends Application implements EventHandler<KeyEvent>
{

  Random random = new Random();
  private final Group root = new Group();
  private final Xform graphXForm = new Xform();
  private final PerspectiveCamera camera = new PerspectiveCamera(true);
  private final Xform cameraXform = new Xform();
  private final Xform cameraXform2 = new Xform();
  private final Xform cameraXform3 = new Xform();
  private final double CAMERA_INITIAL_DISTANCE = -450;
  private double cameraDistance = -450;
  private final double CAMERA_INITIAL_X_ANGLE = 30.0;
  private final double CAMERA_INITIAL_Y_ANGLE = 320.0;
  private final double CAMERA_NEAR_CLIP = 0.1;
  private final double CAMERA_FAR_CLIP = 10000.0;
  private final double LIGHTNING_STRIKE_PROB = 0.001;

  static Cell[][] graph = new Cell[252][252]; //252 for padding
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
    BorderPane pane = new BorderPane();
    Scene topScene = new Scene(pane, 1024, 768, true, SceneAntialiasing.BALANCED);
    topScene.setOnKeyPressed(this);

    SubScene scene = new SubScene(root, 1024, 768, true, SceneAntialiasing.BALANCED);
    scene.setFill(Color.GREY);

    pane.setCenter(scene);
    root.getChildren().add(graphXForm);

    scene.widthProperty().bind(pane.widthProperty());

    buildCamera();
    buildBoard();

    primaryStage.setTitle("Forest Fire");
    primaryStage.setScene(topScene);
    primaryStage.show();

    scene.setCamera(camera);

    loop.start();
  }

  /**
   * This function is used to build the initial board, so the only thing that needs to be called
   * for every cell is toLife() or toDeath().
   */
  private void buildBoard()
  {
    for (int i = 1; i <= 251; i++)
    {
      for (int j = 1; j <= 251; j++)
      {
        nextState[i][j] = 0;
        graph[i][j] = new Cell(i, j, 0);
        graph[i][j].getCell().setTranslateX(i - 125);
        graph[i][j].getCell().setTranslateY(j - 125);
        graphXForm.getChildren().add(graph[i][j].getCell());
      }
    }
    graphXForm.rx.setAngle(cameraXform.rx.getAngle());
    graphXForm.ry.setAngle(cameraXform.ry.getAngle());

    for (int i = 1; i <= 251; i++)
    {
      for (int j = 1; j <= 251; j++)
      {
        graph[i][j].setNeighbors(getNeighbors(i,j));
      }
    }
  }

  private List<Cell> getNeighbors(int i, int j)
  {
    List<Cell> neighbors = new ArrayList<>();

    for(int row = i - 1; row <=  i + 1; row++)
    {
      for(int col = j - 1; col <= j + 1; col++)
      {
        if(row != col)
        {
          if( row > 0 && col > 0 && row < 252 && col < 252)
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
    if (e.getCode() == KeyCode.O)
    {
      cameraDistance -= 2;
      camera.setTranslateZ(cameraDistance);
    } else if (e.getCode() == KeyCode.I)
    {
      cameraDistance += 2;
      camera.setTranslateZ(cameraDistance);
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
   * This is the main game loop that is ran based on when the user clicking the run button.
   */
  class Loop extends AnimationTimer
  {
    int frame = 0;
    private TreeSpecies one = new TreeSpecies(random.nextDouble(), Color.FORESTGREEN);
    private TreeSpecies two = new TreeSpecies(random.nextDouble(), Color.DARKOLIVEGREEN);

    @Override
    public void handle(long time)
    {
      updateGraph();
      //clearDeadCells();
      frame++;
    }

    private void createTwoSpecies()
    {
      one = new TreeSpecies(random.nextDouble(), Color.FORESTGREEN);
      two = new TreeSpecies(random.nextDouble(), Color.DARKOLIVEGREEN);
    }

    private void updateGraph()
    {
      //Updates the state based on the status
      for (int i = 1; i <= 251; i++)
      {
        for (int j = 1; j <= 251; j++)
        {
          int status = graph[i][j].getStatus();
          if(status == 3)
          {
            //On fire, go to barren and set all neighbor trees on fire
            nextState[i][j] = 0;
            for(Cell c : graph[i][j].getNeighbors())
            {
              if(c.getStatus() == 1 || c.getStatus() == 2)
              {
                nextState[c.getX()][c.getY()] = 2;
              }
            }
          }
          //if there is a tree, check for lightning strike
          else if(status == 1 || status == 2)
          {
            if(random.nextDouble() < LIGHTNING_STRIKE_PROB)
            {
              nextState[i][j] = 2;
            }
          }
          //if barren chance to spawn tree
          else if(status == 0)
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
        }
      }

      //Updates cell only when needed
      for (int i = 1; i <= 251; i++)
      {
        for (int j = 1; j <= 251; j++)
        {
          int status = nextState[i][j];
          if (graph[i][j].getStatus() != status)
          {
            if(status == 3)
            {
              graph[i][j].getCell().setFill(Color.RED);
              graph[i][j].getCell().setVisible(true);
              graph[i][j].setStatus(3);
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
              graph[i][j].getCell().setFill(Color.WHITE);
              graph[i][j].getCell().setVisible(false);
              graph[i][j].setStatus(0);
            }
          }
        }
      }
    }

    /**
     * This function clears the dead cells after every time the graph is updated, this helps with the
     * overhead.
     */
    private void clearDeadCells()
    {
      for (int i = 1; i <= 251; i++)
      {
        for (int j = 1; j <= 251; j++)
        {
          int status = graph[i][j].getStatus();
          if (status == 0)
          {
            graph[i][j].getCell().setVisible(false);
          }
        }
      }
    }
  }


  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) { launch(args); }

}

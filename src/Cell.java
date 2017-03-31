/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;


/**
 * This class is use to represent a single cell in the graph.
 *
 * @author joshu
 */
final class Cell
{
  private List<Cell> neighbors;
  private final Integer X;
  private final Integer Y;
  //private Rectangle cell = new Rectangle(0.1, 0.1);
  private Rectangle cell = new Rectangle(1, 1);
  private Integer status;

  /**
   * This creates the actual cell itself which is a rectangle.
   *
   * @param xpos   the x position of the cell
   * @param ypos   the y position of the cell
   * @param status the status 0 = barren, 1 = tree One, 2 = tree Two, 3 = on Fire, 4 = put out by firefighter
   */
  Cell(int xpos, int ypos, int status)
  {
    this.X = xpos;
    this.Y = ypos;
    this.status = status;
    if (status == 0)
    {
      cell.setVisible(false);
    }
    cell.setFill(Color.WHITE);
  }


  /**
   * @return the status of this cell
   */
  public Integer getStatus()
  {
    return this.status;
  }

  public Rectangle getCell() { return this.cell; }

  public List<Cell> getNeighbors() { return this.neighbors; }

  public void setNeighbors(List<Cell> neighbors) { this.neighbors = neighbors; }

  public int getX() { return this.X; }

  public int getY() { return this.Y; }

  public void setStatus(int status) { this.status = status; }
}

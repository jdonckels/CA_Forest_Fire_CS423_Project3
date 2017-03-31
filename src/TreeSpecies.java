import javafx.scene.paint.Color;

/**
 * Created by joshu on 3/29/2017.
 */
public class TreeSpecies {
    private double probability;
    private double longevity;
    private double biomass;
    private Color color;
    private double fitness;

    public TreeSpecies(double probability, Color color)
    {
        longevity = 0;
        biomass = 0;
        this.probability = probability;
        this.color = color;
        fitness = 0;
    }

    public double getProbability() { return this.probability; }

    public double getLongevity() { return this.longevity; }

    public double getBiomass() { return this.biomass; }

    public void setLongevity(double value) { this.longevity = value; }

    public void setBiomass(double value) { this.biomass = value; }

    public Color getColor() { return this.color; }

    public double getFitness() { return this.fitness; }

    public void setFitness(double fitness) { this.fitness = fitness; }
}

This code was created by Joshua Donckels and Michael Wolcott.

To run this program one must simply open it up in intellij or any other editor, and run it from there.

This program has many different running arguments, but the version that it is right now does not support command line arguments.

To change what the program does there are many booleans as well as variables located at the top of main that will change what the program does.
The options are: (no input arguments)

  - boolean twoSpecies =
    - True: two species evolving
    - False: one species
  - boolean SLOW =
      - True: Slows down the gui significantly
      - False: normal speed
  - boolean GUI =
      - True: Has a graphic for the user
      - False: no graphic
  - boolean DEBUG =
      - True: prints more output
      - False: does not print anything
  - boolean FIREFIGHTERS =
      - True: firefighters are enabled for the model
      - False: no firefighters
  - boolean GA =
      - True: uses a GA to evolve
      - False: steps through increading p-values each iteration
  - int MAX_STEPS = number of steps that the model will take each iteration
  - int numberOfFireFighters = number of firefighters the model will contain
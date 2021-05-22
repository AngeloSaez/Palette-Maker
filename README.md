![Logo](https://user-images.githubusercontent.com/64299151/119223353-ef89ab80-bac6-11eb-9857-4d597798e79e.png)
# Palette-Maker
Palette Maker


Generates palettes in a simple and visual way. Afterwards exports it to your desktop as an image.




Controls:
  - Use Left / Right arrows to increase / decrease choices respectively
  - Use Up / Down arrows at pretty much any time to offset hues
  - Use Enter to submit your choices and progress process. On the last step it exports the image and closes the program.




Palette Generation Pipeline:
1. Pick hue derivation method
2 Pick hue count
3 Pick swatch/value count
4 Adjust saturation
5 Adjust brightness
6 Adjust tint
7 Pick render style
8 Export





Summary of each step:
1. Pick hue derivation method
  - There are multiple ways to come up with what the hues will be given. The default setting is equidistant hues.

2 Pick hue count
  - Pick the distinct hues to be used for each of the subsequent steps.
  
3.Pick swatch/value count
  - Pick the length of the gradient for each hue between complete black and complete white.

4.Adjust saturation
  - Starting from 100% saturation decrease as needed.
  
5.Adjust brightness
  - Starting from 100% brightness decrease as needed.
   
6.Adjust tint
  - Adjust the overall tint of the whole palette. Even effects the black and white colors. Takes a weighted average of each swatch and the tint.

7.Pick render style
  - Decide if you want to include gradients between hues or to use the basic rendering style.

8.Export
  - Exports to your desktop as a png of a decently large resolution. If you have a folder named 'palettes' on your desktop, it will export there instead.

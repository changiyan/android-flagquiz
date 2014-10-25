// QuizFragment.java
// Contains the Flag Quiz logic
package com.deitel.flagquiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuizFragment extends Fragment 
{
   // String used when logging error messages
   private static final String TAG = "FlagQuiz Activity";

   private static final int FLAGS_IN_QUIZ = 10; 
   
   private List<String> fileNameList; // flag file names
   private List<String> quizCountriesList; // countries in current quiz
   private Set<String> regionsSet; // world regions in current quiz
   private String correctAnswer; // correct country for the current flag
   private int numGuesses; // guesses on question
   private int firstGuess; // number of first guess successes
   private int totalGuesses; // number of guesses made
   private int correctAnswers; // number of correct guesses
   private int guessRows; // number of rows displaying guess Buttons
   private int numPlayers; // number of players
   private int score; // keep track of score
   private int p2score; // for multiplayer
   private boolean capitalChance; // chance to get capital
   private boolean playerTurn; // true is p1, false is p2
   private SharedPreferences highScore; // saves high score
   private SecureRandom random; // used to randomize the quiz
   private Animation shakeAnimation; // animation for incorrect guess
   
   private TextView questionNumberTextView; // shows current question #
   private ImageView flagImageView; // displays a flag
   private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
   private TextView answerTextView; // displays Correct! or Incorrect!
   private TextView scoreTextView; // displays score
   private TextView p2ScoreTextView; // score for player 2
   private Button nextButton; // button for next question
   private Button linkButton; // button for link
   
   // configures the QuizFragment when its View is created
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState)
   {
      super.onCreateView(inflater, container, savedInstanceState);    
      View view = 
         inflater.inflate(R.layout.fragment_quiz, container, false);
   // attempting shared preferences for high score
      highScore = this.getActivity().getSharedPreferences("pref", 0);

      fileNameList = new ArrayList<String>();
      quizCountriesList = new ArrayList<String>();
      random = new SecureRandom(); 

      // load the shake animation that's used for incorrect answers
      shakeAnimation = AnimationUtils.loadAnimation(getActivity(), 
         R.anim.incorrect_shake); 
      shakeAnimation.setRepeatCount(3); // animation repeats 3 times 

      // get references to GUI components
      questionNumberTextView = 
         (TextView) view.findViewById(R.id.questionNumberTextView);
      flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
      guessLinearLayouts = new LinearLayout[3];
      guessLinearLayouts[0] = 
         (LinearLayout) view.findViewById(R.id.row1LinearLayout);
      guessLinearLayouts[1] = 
         (LinearLayout) view.findViewById(R.id.row2LinearLayout);
      guessLinearLayouts[2] = 
         (LinearLayout) view.findViewById(R.id.row3LinearLayout);
      answerTextView = (TextView) view.findViewById(R.id.answerTextView);
      scoreTextView = (TextView) view.findViewById(R.id.scoreTextView);
      p2ScoreTextView = (TextView) view.findViewById(R.id.p2ScoreTextView);
      nextButton = (Button) view.findViewById(R.id.nextButton);
      nextButton.setOnClickListener(nextButtonListener);

      linkButton = (Button) view.findViewById(R.id.linkButton);
      linkButton.setOnClickListener(linkButtonListener);
      
      // configure listeners for the guess Buttons
      for (LinearLayout row : guessLinearLayouts)
      {
         for (int column = 0; column < row.getChildCount(); column++) 
         {
            Button button = (Button) row.getChildAt(column);
            button.setOnClickListener(guessButtonListener);
         }
      }  
      
      // set questionNumberTextView's text
      questionNumberTextView.setText(
         getResources().getString(R.string.question, 1, FLAGS_IN_QUIZ));
      return view; // returns the fragment's view for display
   } // end method onCreateView
   
   // update guessRows based on value in SharedPreferences
   public void updateGuessRows(SharedPreferences sharedPreferences)
   {
      // get the number of guess buttons that should be displayed
      String choices = 
         sharedPreferences.getString(MainActivity.CHOICES, null);
      guessRows = Integer.parseInt(choices) / 3;

      // hide all guess button LinearLayouts
      for (LinearLayout layout : guessLinearLayouts)
         layout.setVisibility(View.INVISIBLE);

      // display appropriate guess button LinearLayouts 
      for (int row = 0; row < guessRows; row++) 
         guessLinearLayouts[row].setVisibility(View.VISIBLE);
   }
   
   // update world regions for quiz based on values in SharedPreferences
   public void updateRegions(SharedPreferences sharedPreferences)
   {
      regionsSet = 
         sharedPreferences.getStringSet(MainActivity.REGIONS, null);
   }  

   public void updatePlayers(SharedPreferences sharedPreferences){
	   String multiplayer =
			   sharedPreferences.getString(MainActivity.PLAYERS, null);
	   numPlayers = Integer.parseInt(multiplayer);
   }
   
   // set up and start the next quiz 
   public void resetQuiz() 
   {      
      // use AssetManager to get image file names for enabled regions
      AssetManager assets = getActivity().getAssets(); 
      fileNameList.clear(); // empty list of image file names
      
      try 
      {
         // loop through each region
         for (String region : regionsSet) 
         {
            // get a list of all flag image files in this region
            String[] paths = assets.list(region);

            for (String path : paths) 
               fileNameList.add(path.replace(".png", ""));
         } 
      } 
      catch (IOException exception) 
      {
         Log.e(TAG, "Error loading image file names", exception);
      } 
      
      correctAnswers = 0; // reset the number of correct answers made
      totalGuesses = 0; // reset the total number of guesses the user made
      numGuesses = 0; // reset number guess for question
      firstGuess = 0; // reset first guesses
      score = 0; // reset score
      p2score=0;
      capitalChance = false;
      playerTurn = true;
      quizCountriesList.clear(); // clear prior list of quiz countries
      scoreTextView.setText(""+score);
      if(numPlayers == 1)
    	  p2ScoreTextView.setText("");
      else
    	  p2ScoreTextView.setText(""+p2score);
            
      int flagCounter = 1; 
      int numberOfFlags = fileNameList.size(); 

      // add FLAGS_IN_QUIZ random file names to the quizCountriesList
      while (flagCounter <= FLAGS_IN_QUIZ) 
      {
         int randomIndex = random.nextInt(numberOfFlags); 

         // get the random file name
         String fileName = fileNameList.get(randomIndex);
         
         // if the region is enabled and it hasn't already been chosen
         if (!quizCountriesList.contains(fileName)) 
         {
            quizCountriesList.add(fileName); // add the file to the list
            ++flagCounter;
         } 
      } 

      loadNextFlag(); // start the quiz by loading the first flag
   } // end method resetQuiz

   // after the user guesses a correct flag, load the next flag
   private void loadNextFlag() 
   {
      // get file name of the next flag and remove it from the list
	   nextButton.setEnabled(false);
	   linkButton.setEnabled(false);
	   capitalChance = false;
      String nextImage = quizCountriesList.remove(0);
      correctAnswer = nextImage; // update the correct answer
      answerTextView.setText(""); // clear answerTextView 

      // display current question number
      questionNumberTextView.setText(
         getResources().getString(R.string.question, 
            (correctAnswers + 1), FLAGS_IN_QUIZ));

      // extract the region from the next image's name
      String region = nextImage.substring(0, nextImage.indexOf('-'));

      // use AssetManager to load next image from assets folder
      AssetManager assets = getActivity().getAssets(); 

      try
      {
         // get an InputStream to the asset representing the next flag
         InputStream stream = 
            assets.open(region + "/" + nextImage + ".png");
         
         // load the asset as a Drawable and display on the flagImageView
         Drawable flag = Drawable.createFromStream(stream, nextImage);
         flagImageView.setImageDrawable(flag);                       
      } 
      catch (IOException exception)  
      {
         Log.e(TAG, "Error loading " + nextImage, exception);
      } 

      Collections.shuffle(fileNameList); // shuffle file names

      // put the correct answer at the end of fileNameList
      int correct = fileNameList.indexOf(correctAnswer);
      fileNameList.add(fileNameList.remove(correct));

      // add 3, 6, or 9 guess Buttons based on the value of guessRows
      for (int row = 0; row < guessRows; row++) 
      {
         // place Buttons in currentTableRow
         for (int column = 0; 
            column < guessLinearLayouts[row].getChildCount(); column++) 
         { 
            // get reference to Button to configure
            Button newGuessButton = 
               (Button) guessLinearLayouts[row].getChildAt(column);
            newGuessButton.setEnabled(true);

            // get country name and set it as newGuessButton's text
            String fileName = fileNameList.get((row * 3) + column);
            newGuessButton.setText(getCountryName(fileName));
         } 
      } 
      
      // randomly replace one Button with the correct answer
      int row = random.nextInt(guessRows); // pick random row
      int column = random.nextInt(3); // pick random column
      LinearLayout randomRow = guessLinearLayouts[row]; // get the row
      String countryName = getCountryName(correctAnswer);
      ((Button) randomRow.getChildAt(column)).setText(countryName);    
   } // end method loadNextFlag

   private void loadCapital(){
	   capitalChance = true;
	   questionNumberTextView.setText("Can You Guess the Capital?");
	   for (int row = 0; row < guessRows; row++) 
	      {
	         // place Buttons in currentTableRow
	         for (int column = 0; 
	            column < guessLinearLayouts[row].getChildCount(); column++) 
	         { 
	            // get reference to Button to configure
	            Button newGuessButton = 
	               (Button) guessLinearLayouts[row].getChildAt(column);
	            newGuessButton.setEnabled(true);

	            // get country name and set it as newGuessButton's text
	            String fileName = fileNameList.get((row * 3) + column);
	            newGuessButton.setText(getCapitalName(fileName));
	         } 
	      } 
	      
	      // randomly replace one Button with the correct answer
	      int row = random.nextInt(guessRows); // pick random row
	      int column = random.nextInt(3); // pick random column
	      LinearLayout randomRow = guessLinearLayouts[row]; // get the row
	      String capitalName = getCapitalName(correctAnswer);
	      ((Button) randomRow.getChildAt(column)).setText(capitalName);
   }
   
   // parses the country flag file name and returns the country name
   private String getCountryName(String name)
   {
      return name.substring(name.indexOf('-') + 1, name.lastIndexOf('-')).replace('_', ' ');
   } 
   
   private String getCountrySlug(String name){
	   return name.substring(name.indexOf('-') + 1, name.lastIndexOf('-'));
   }
   
   private String getCapitalName(String name)
   {
      return name.substring(name.lastIndexOf('-')+1).replace('_', ' ');
   }
   
   // called when a guess Button is touched
   private OnClickListener guessButtonListener = new OnClickListener() 
   {
      @Override
      public void onClick(View v) 
      {
         Button guessButton = ((Button) v); 
         String guess = guessButton.getText().toString();
         String answer = getCountryName(correctAnswer);
         String capital = getCapitalName(correctAnswer);
         if(!capitalChance && numPlayers == 1){
        	 ++totalGuesses; // increment number of guesses the user has made
        	 ++numGuesses; // increment guess for question
         }
         
         if (guess.equals(answer)) // if the guess is correct
         {
        	 if (numPlayers == 1){
	        	 if(numGuesses == 1){
	        		 ++firstGuess; // increment first guess if only one guess
	        		 score += 100;
				}
				else // each guess subtracts 5 points from 100
					score += 100 - 5*numGuesses;
        	 }
        	 else if(playerTurn)
        		 score +=100;
        	 else{
        		 p2score += 100;
        		 p2ScoreTextView.setText(""+p2score);
        	 }	 
            ++correctAnswers; // increment the number of correct answers

            // display correct answer in green text
            scoreTextView.setText(""+score);
            answerTextView.setText(answer + "!");
            answerTextView.setTextColor(
               getResources().getColor(R.color.correct_answer));
            loadCapital();
         }
         else if(capitalChance){
        	// +10 for correct capital
        	 if(guess.equals(capital)){
        		 if(playerTurn){
	        		score += 10;
	        		scoreTextView.setText(""+score);
        		 } // will be true even singleplayer
        		 else{
        			 p2score += 10;
        			 p2ScoreTextView.setText(""+p2score);
        		 }
 	            answerTextView.setText(capital + "!");
 	            answerTextView.setTextColor(
 	               getResources().getColor(R.color.correct_answer));
        	 }
        	 else{
        		 answerTextView.setText(capital + "!");
        		 answerTextView.setTextColor(
  	 	               getResources().getColor(R.color.incorrect_answer));
        	 } 
        	 if(numPlayers == 1)
        		 numGuesses = 0; // reset it for next question
        	 else
        		 playerTurn = !playerTurn; // change to next player
            disableButtons(); // disable all guess Buttons
            
            // if the user has correctly identified FLAGS_IN_QUIZ flags
            if (correctAnswers == FLAGS_IN_QUIZ) 
            {
            	if(numPlayers == 1){
            		setHighScore(score);
	               // DialogFragment to display quiz stats and start new quiz
	               DialogFragment quizResults = 
	                  new DialogFragment()
	                  {
	                     // create an AlertDialog and return it
	                     @Override
	                     public Dialog onCreateDialog(Bundle bundle)
	                     {
	                    	int first = highScore.getInt("first", 0);
	 						int second = highScore.getInt("second", 0);
	 						int third = highScore.getInt("third", 0);
	 						int fourth = highScore.getInt("fourth", 0);
	 						int fifth = highScore.getInt("fifth", 0);
	                        AlertDialog.Builder builder = 
	                           new AlertDialog.Builder(getActivity());
	                        builder.setCancelable(false); 
	                        
	                        // displays stats and high scores
	                        builder.setMessage(
	                           getResources().getString(R.string.results, 
	                           totalGuesses, (1000 / (double) totalGuesses), 
	                           firstGuess, ((double) firstGuess / 10),
	                           first, second, third, fourth, fifth));
	                        
	                        // "Reset Quiz" Button                              
	                        builder.setPositiveButton(R.string.reset_quiz,
	                           new DialogInterface.OnClickListener()                
	                           {                                                       
	                              public void onClick(DialogInterface dialog, 
	                                 int id) 
	                              {
	                                 resetQuiz();                                      
	                              } 
	                           } // end anonymous inner class
	                        ); // end call to setPositiveButton
	                        
	                        return builder.create(); // return the AlertDialog
	                     } // end method onCreateDialog   
	                  }; // end DialogFragment anonymous inner class
	               
	               // use FragmentManager to display the DialogFragment
	               quizResults.show(getFragmentManager(), "quiz results");
            	} // single player results
            	else{

            		 DialogFragment quizResults = 
       	                  new DialogFragment()
       	                  {
       	                     // create an AlertDialog and return it
       	                     @Override
       	                     public Dialog onCreateDialog(Bundle bundle)
       	                     {
       	                    	 
       	                    	int winner;
       	                    	boolean draw = false;
		       	             		if (score>p2score)
		       	             			winner = 1;
		       	             		else if (score<p2score)
		       	             			winner = 2;
		       	             		else{
		       	             			draw = true;
		       	             			winner = 0;
		       	             		} // tie
       	                        AlertDialog.Builder builder = 
       	                           new AlertDialog.Builder(getActivity());
       	                        builder.setCancelable(false); 
       	                        
       	                        if(draw){
       	                        	builder.setMessage(
   	                        			getResources().getString(R.string.mTie, 
   	             	                           score, p2score));
       	                        }
       	                        // displays stats and high scores
       	                        else{
       	                        builder.setMessage(
       	                           getResources().getString(R.string.mResults, 
       	                           score, p2score, winner));
       	                        }
       	                        
       	                        // "Reset Quiz" Button                              
       	                        builder.setPositiveButton(R.string.reset_quiz,
       	                           new DialogInterface.OnClickListener()                
       	                           {                                                       
       	                              public void onClick(DialogInterface dialog, 
       	                                 int id) 
       	                              {
       	                                 resetQuiz();                                      
       	                              } 
       	                           } // end anonymous inner class
       	                        ); // end call to setPositiveButton
       	                        
       	                        return builder.create(); // return the AlertDialog
       	                     } // end method onCreateDialog   
       	                  }; // end DialogFragment anonymous inner class
       	               
       	               // use FragmentManager to display the DialogFragment
       	               quizResults.show(getFragmentManager(), "quiz results");
            	} // multiplayer results, only shows who wins
            } 
            else // answer is correct but quiz is not over 
            {
               nextButton.setEnabled(true);
               linkButton.setEnabled(true);
               if(numPlayers == 2)
            	   playerTurn = !playerTurn;
            } 
         } 
         else // guess was incorrect  
         {
            flagImageView.startAnimation(shakeAnimation); // play shake

            // display "Incorrect!" in red 
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(
               getResources().getColor(R.color.incorrect_answer));
            guessButton.setEnabled(false); // disable incorrect answer
            if(numPlayers == 2)
            	playerTurn = !playerTurn;
         } 
      } // end method onClick  
   }; // end answerButtonListener

   private OnClickListener nextButtonListener = new OnClickListener(){
	   @Override
	   public void onClick(View v){
		   loadNextFlag();
	   }
   };
   
   private OnClickListener linkButtonListener = new OnClickListener(){
		@Override
		public void onClick(View v){
			String urlString = getString(R.string.searchURL)+
					getCountrySlug(correctAnswer); // wiki + slug
			
			Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
			
			startActivity(webIntent); // launches web
		} // launch browser to display results
	};
   
   // utility method that disables all answer Buttons 
   private void disableButtons()
   {
      for (int row = 0; row < guessRows; row++)
      {
         LinearLayout guessRow = guessLinearLayouts[row];
         for (int i = 0; i < guessRow.getChildCount(); i++)
            guessRow.getChildAt(i).setEnabled(false);
      } 
   } 
   
   // sets high score
   private void setHighScore(int score){
		int first = highScore.getInt("first", 0);
		int second = highScore.getInt("second", 0);
		int third = highScore.getInt("third", 0);
		int fourth = highScore.getInt("fourth", 0);
		int fifth = highScore.getInt("fifth", 0);
		if(score < fifth)
			return;
		SharedPreferences.Editor hsEditor = highScore.edit();
		if (score > first){
			hsEditor.putInt("first", score);
			hsEditor.putInt("second", first);
			hsEditor.putInt("third", second);
			hsEditor.putInt("fourth", third);
			hsEditor.putInt("fifth", fourth);
		} // greatest score
		else if (score > second){
			hsEditor.putInt("first", first);
			hsEditor.putInt("second", score);
			hsEditor.putInt("third", second);
			hsEditor.putInt("fourth", third);
			hsEditor.putInt("fifth", fourth);
		} // if not first, second
		else if (score > third){
			hsEditor.putInt("first", first);
			hsEditor.putInt("second", second);
			hsEditor.putInt("third", score);
			hsEditor.putInt("fourth", third);
			hsEditor.putInt("fifth", fourth);
		} // third
		else if (score > fourth){
			hsEditor.putInt("first", first);
			hsEditor.putInt("second", second);
			hsEditor.putInt("third", third);
			hsEditor.putInt("fourth", score);
			hsEditor.putInt("fifth", fourth);
		} // fourth
		hsEditor.apply();
	}
} // end class FlagQuiz

     
/*************************************************************************
* (C) Copyright 1992-2014 by Deitel & Associates, Inc. and               *
* Pearson Education, Inc. All Rights Reserved.                           *
*                                                                        *
* DISCLAIMER: The authors and publisher of this book have used their     *
* best efforts in preparing the book. These efforts include the          *
* development, research, and testing of the theories and programs        *
* to determine their effectiveness. The authors and publisher make       *
* no warranty of any kind, expressed or implied, with regard to these    *
* programs or to the documentation contained in these books. The authors *
* and publisher shall not be liable in any event for incidental or       *
* consequential damages in connection with, or arising out of, the       *
* furnishing, performance, or use of these programs.                     *
*************************************************************************/

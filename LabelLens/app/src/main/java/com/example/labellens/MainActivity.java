package com.example.labellens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button button;
    private Uri photoUri;
    private File photoFile;
    private TextView resultText;
    private File createImageFile() throws IOException{
        String fileName = "captured_image_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result){
                    String imagePath = photoFile.getAbsolutePath();
                    resultText = findViewById(R.id.resultsText);
                    resultText.setText("Analisando...");
                    imageView.setImageURI(photoUri);
                    new Thread(() -> {
                        try{
                            ServiceApi serviceApi = new ServiceApi(imagePath);
                            FoodNutrients nutrients = serviceApi.getFoodNutrients();

                            runOnUiThread(() -> {
                               Log.d("API", nutrients.toString());
                               Double qualityScore = calculateQualityScore(nutrients);
                               String quality;
                               resultText.setText("Score de Qualidade do Alimento: " + String.format("%.1f", qualityScore) + "\n");
                               if(qualityScore >= 75){
                                   quality = "A - Excelente";
                               } else if(qualityScore >= 50){
                                   quality = "B - Bom";
                               } else if(qualityScore >= 25){
                                   quality = "C - Regular";
                               } else{
                                   quality = "D - Consumir com moderação";
                               }
                               if(qualityScore >= 50){
                                   resultText.setTextColor(Color.GREEN);
                               } else{
                                   resultText.setTextColor(Color.RED);
                               }
                               resultText.setText(resultText.getText() + "Classificação: " + quality + "\nCalorias: " + nutrients.getValorEnergetico().getPor100g());
                            });
                        } catch (Exception e){
                            runOnUiThread(() -> resultText.setText("Erro ao analisar a imagem"));
                        }
                    }).start();
                }
            });

    private void openCamera(){
        try {
            photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    photoFile
            );
            takePictureLauncher.launch(photoUri);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.capturedImage);
        button = findViewById(R.id.openCamera);

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openCamera();
            }
        });
    }

    private double calculateQualityScore(FoodNutrients food){
        double score = 50;
        score += (parseVd(food.getFibrasAlimentares()) * 2 + parseVd(food.getProteinas()) * 1.5 + parseVd(food.getCalcio()) * 0.5 - parseVd(food.getGordurasSaturadas()) * 1.5 - parseVd(food.getAcucaresAdicionados()) * 2 - parseVd(food.getSodio()) - parseVd(food.getGordurasTrans()) * 10)/17.5;
        return score;
    }

    private double parseVd(NutrientDetail nutrient){
        if(nutrient == null || nutrient.getVdPorcao() == null || nutrient.getVdPorcao().isEmpty()){
            return 0.0;
        }
        try {
            String vdString = nutrient.getVdPorcao().replaceAll("[^0-9,]", "").replace(",",".");
            if (!vdString.isEmpty()){
            return Double.parseDouble(vdString);
            }
            return 0.0;
        } catch (NumberFormatException e){
            return 0.0;
        }
    }

}

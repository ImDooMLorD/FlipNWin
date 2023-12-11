package com.imdoomlord.mymemory

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.imdoomlord.mymemory.models.BoardSize
import com.imdoomlord.mymemory.models.MemoryGame

class MainActivity : AppCompatActivity() {

    companion object{
        private const val TAG = "MainActivity"
    }

    private lateinit var clRoot: ConstraintLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    //? memoryGame and adapter is made as property of MainActivity class ->> *** TO Make them usable in other functions/....
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter

    private var boardSize: BoardSize = BoardSize.HARD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        memoryGame = MemoryGame(boardSize)

        //Adapter -- binds data into views in RecyclerView -- PROPERTY
        // Adapter initialized with class(MemoryBoardAdapter -- holds data for views) takes context and total layout views
        adapter = MemoryBoardAdapter(this , boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })

        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)       //Performance optimization

        // layoutManager--PROPERTY--RecyclerView
        //GridLayoutManager takes 2 param context(i.e, this == MainActivity here) and row/column i.e, spanCount
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())


    }

    private fun updateGameWithFlip(position: Int) {
        // Error Checking
        if(memoryGame.haveWonGame()){
            Snackbar.make(clRoot, "STOP~!", Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot, "YOU NEED EYES BRAAAAAAH!", Snackbar.LENGTH_SHORT).show()
            return
        }
        // Actually flipping over the card
        if(memoryGame.flipCard(position)){
            Log.i(TAG, "Found a match! Num pairs found: ${memoryGame.numPairsFound}")
            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, "Jeet Gye Re BETE! ", Snackbar.LENGTH_SHORT).show()

            }

        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()

    }
}
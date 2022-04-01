package game2048;

/** Symbolic names for the four sides of a board.
 *  @author P. N. Hilfinger */
public enum Side {
    /** The parameters (COL0, ROW0, DCOL, and DROW) for each of the
     *  symbolic directions, D, below are to be interpreted as follows:
     *     The board's standard orientation has the top of the board
     *     as NORTH, and rows and columns (see Model) are numbered
     *     from its lower-left corner. Consider the board oriented
     *     so that side D of the board is farthest from you. Then
     *        * (COL0*s, ROW0*s) are the standard coordinates of the
     *          lower-left corner of the reoriented board (where s is the
     *          board size), and
     *        * If (c, r) are the standard coordinates of a certain
     *          square on the reoriented board, then (c+DCOL, r+DROW)
     *          are the standard coordinates of the squares immediately
     *          above it on the reoriented board.
     *  The idea behind going to this trouble is that by using the
     *  col() and row() methods below to translate from reoriented to
     *  standard coordinates, one can arrange to use exactly the same code
     *  to compute the result of tilting the board in any particular
     *  direction. */
    /**
     * comment by fanfei
     * col0和row0是用来表示东西南北四个方向的
     * 如何在坐标轴上表示东西南北四个方向的坐标？
     * 作者使用的方法是用与该方向相对的左下角这个点的坐标来表示四个方向
     * 首先，假设棋盘的左下角在坐标轴原点，此时北方向的这个边离我们最远，而当前左下角的坐标是(0, 0)
     * 所以作者用col0=0, row0=0来表示北
     * 类似，假设我们换了一个方向，站在左侧（西侧）这个边上，此时东方向的那个边离我们最远，而此时在我们的视角里左下角的坐标是(0,1)
     * (假设棋盘的长度s为1)，所以作者用col0=0, row0=1来表示东
     *
     * 这里有个疑问，为什么不把坐标轴建在正方形的中心，然后使用(0, 1)表示北、(0,-1)表示南呢？
     *
     * dcol和drow的用处暂时不明，可能是为了快速计算以不同方向为主方向时图中点的坐标而设计的
     * dcol和drow这两个值是将正方体向主方向的反方向移动一个距离，正方体坐标所需要进行的调整的值
     * 怎么感觉和我上面写的疑问那里建立坐标轴的方式相同?
     */

    NORTH(0, 0, 0, 1), EAST(0, 1, 1, 0), SOUTH(1, 1, 0, -1),
    WEST(1, 0, -1, 0);

    /** The side that is in the direction (DCOL, DROW) from any square
     *  of the board.  Here, "direction (DCOL, DROW) means that to
     *  move one space in the direction of this Side increases the row
     *  by DROW and the colunn by DCOL.  (COL0, ROW0) are the row and
     *  column of the lower-left square when sitting at the board facing
     *  towards this Side. */
    Side(int col0, int row0, int dcol, int drow) {
        this.row0 = row0;
        this.col0 = col0;
        this.drow = drow;
        this.dcol = dcol;
    }

    /** Returns the side opposite of side S. */
    static Side opposite(Side s) {
        if (s == NORTH) {
            return SOUTH;
        } else if (s == SOUTH) {
            return NORTH;
        } else if (s == EAST) {
            return WEST;
        } else {
            return EAST;
        }
    }

    /** Return the standard column number for square (C, R) on a board
     *  of size SIZE oriented with this Side on top. */
    public int col(int c, int r, int size) {
        return col0 * (size - 1) + c * drow + r * dcol;
    }

    /** Return the standard row number for square (C, R) on a board
     *  of size SIZE oriented with this Side on top. */
    public int row(int c, int r, int size) {
        return row0 * (size - 1) - c * dcol + r * drow;
    }

    /** Parameters describing this Side, as documented in the comment at the
     *  start of this class. */
    private int row0, col0, drow, dcol;

};

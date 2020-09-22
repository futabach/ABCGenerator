import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class ABCPlace {

	public int GAME_TYPE;
	public int GAME_SIZE;
	public int GAME_COLOR;
	public int[][] map;
	public int[][][] hintMap;
	private int[][] blankMap;
	private int[] baseMap;
	private int[] hintMaplen;
	/**
	 * 指定サイズ、指定色数で、
	 * ヒントエリアに重複が無い、解答が1意なQuizを生成する
	 * @param GAME_SIZE
	 * @param GAME_COLOR
	 * @return
	 */
	public ABCPlace makeQuiz(int GAME_TYPE,int GAME_SIZE, int GAME_COLOR) {
		// 有り得ない引数が渡された場合は打ち切り
		if(GAME_TYPE != 3 &&GAME_TYPE != 4 && GAME_TYPE != 6) {
			throw new IllegalArgumentException("GAME_TYPEが異常");
		} else if(GAME_TYPE == 3 && GAME_SIZE % 2 == 1) {
			throw new IllegalArgumentException("GAME_TYPE=3の場合GAME_SIZEは偶数のみ指定可能");
		} else if(GAME_TYPE == 6 && GAME_SIZE % 2 == 0) {
			throw new IllegalArgumentException("GAME_TYPE=6の場合GAME_SIZEは奇数のみ指定可能");
		} else if(GAME_TYPE == 4 && GAME_SIZE < GAME_COLOR ) {
			throw new IllegalArgumentException("GAME_TYPE=4の場合GAME_SIZE < GAME_COLORは不可");
		} else if(GAME_TYPE == 3 && GAME_SIZE + 1 < GAME_COLOR ) {
			throw new IllegalArgumentException("GAME_TYPE=3の場合GAME_SIZE + 1 < GAME_COLORは不可");
		} else if(GAME_TYPE == 6 && GAME_SIZE + 1 < GAME_COLOR * 2 ) {
			throw new IllegalArgumentException("GAME_TYPE=6の場合GAME_SIZE + 1 < GAME_COLOR * 2は不可");
		}
		ABCPlace f = new ABCPlace();
		while(true) {
			f = makeMap(GAME_TYPE,GAME_SIZE, GAME_COLOR);
			// 解が一意の場合は終了
			if(f.isUniqueAnswer()==1)break;
			// 解が一意でない場合はやり直し
			continue;
		}
		// GameType3以外なら終了
		if(GAME_TYPE!=3)return f;
		// ヒントエリアを削る 左右回転の混在版
		return f.shaveType3Hints();
	}

	/**
	 * より難易度の高い問題を作成する。
	 * 不可能な場合（既に最難の場合）false
	 * @return
	 */
	public boolean makeMoreDifficult() {
		boolean flg = false;
		// 正解パターンを取得
		ABCPlace answer = makeFirstAnswer();
		// 別解リストを生成
		ArrayList<ABCPlace> anotherAnswerList = new ArrayList<ABCPlace>();
		while(true) {
			if(!shaveOffHints(answer,anotherAnswerList))break;
			flg = true;
		}
		// 終了前にmapを正解状態にする
		ABCPlace f = makeFirstAnswer();
		DeepCopyToThis(f);
		return flg;
	}
	/**
	 * 解答が一意か確認する。
	 * @return 0:解無し , 1:一意の解 , 2:複数解あり
	 */
	public int isUniqueAnswer() {
		return isUniqueAnswer(null,null);
	}
	/**
	 * 1つ目の答えを取得する（答えが存在しない場合はnull）
	 * @return
	 */
	public ABCPlace makeFirstAnswer()
	{
		return makeAnswer(null);
	}

	/**
	 * 全要素コピー用
	 * @return
	 */
	private ABCPlace DeepCopy()
	{
		ABCPlace obj = new ABCPlace();
		obj.GAME_TYPE = this.GAME_TYPE;
		obj.GAME_SIZE = this.GAME_SIZE;
		obj.GAME_COLOR = this.GAME_COLOR;
		obj.map = new int[GAME_COLOR][GAME_SIZE];
		obj.blankMap = new int[GAME_COLOR][GAME_SIZE];
		obj.baseMap = this.baseMap.clone();
		for (int c = 0; c < GAME_COLOR; c++)
		{
			obj.blankMap[c] = this.blankMap[c].clone();
			obj.map[c] = this.map[c].clone();
		}
		obj.hintMaplen = this.hintMaplen.clone();
		obj.hintMap = new int[hintMaplen[0]][hintMaplen[1]][hintMaplen[2]];
		for (int r = 0; r < hintMaplen[0]; r++) {
			for (int n = 0; n < hintMaplen[1]; n++) {
				obj.hintMap[r][n] = this.hintMap[r][n].clone();
			}
		}
		return obj;
	}
	/**
	 * 全要素を自身へコピー
	 * @return
	 */
	private void DeepCopyToThis(ABCPlace f)
	{
		this.GAME_TYPE = f.GAME_TYPE;
		this.GAME_SIZE = f.GAME_SIZE;
		this.GAME_COLOR = f.GAME_COLOR;
		this.map = new int[GAME_COLOR][GAME_SIZE];
		this.blankMap = new int[GAME_COLOR][GAME_SIZE];
		this.baseMap = f.baseMap.clone();
		for (int c = 0; c < GAME_COLOR; c++)
		{
			this.blankMap[c] = f.blankMap[c].clone();
			this.map[c] = f.map[c].clone();
		}
		this.hintMaplen = f.hintMaplen.clone();
		this.hintMap = new int[hintMaplen[0]][hintMaplen[1]][hintMaplen[2]];
		for (int r = 0; r < hintMaplen[0]; r++) {
			for (int n = 0; n < hintMaplen[1]; n++) {
				this.hintMap[r][n] = f.hintMap[r][n].clone();
			}
		}
	}
	/**
	 * 解答が一意か確認する。
	 * @param firstAnswer　正解が既に算出済みの場合指定
	 * @param anotherAnswerList　不正解リストが存在する場合指定
	 * @return 0:解無し , 1:一意の解 , 2:複数解あり
	 */
	private int isUniqueAnswer(ABCPlace firstAnswer,ArrayList<ABCPlace> anotherAnswerList) {

		// 別解リストが指定されている場合、リスト内に存在するかチェック
		// リスト内に存在する場合は「複数解あり」で終了
		if(anotherAnswerList!=null
				&& !anotherAnswerList.isEmpty()
				&& isInListSameAnswer(anotherAnswerList)
				)return 2;
		// 正解が指定されてい無い場合は探索し、解が存在しない場合0を返し終了
		if(firstAnswer==null) {
			firstAnswer = DeepCopy();
			firstAnswer = firstAnswer.makeFirstAnswer();
			if(firstAnswer == null)return 0;
		}
		ABCPlace anotherA = makeAnswer(firstAnswer);
		// 別解が存在しない場合は解一意で終了
		if(anotherA == null) return 1;
		// 別解リストが指定されている場合、別解をリストへ入れた上で終了
		if(anotherAnswerList !=null)anotherAnswerList.add(anotherA);
		return 2;

	}
	/**
	 * 答えを生成する(f==nullなら1つ目の解を取得する、f!=nullならf以外の解を取得する)
	 * @param f
	 * @return
	 */
	private ABCPlace makeAnswer(ABCPlace f){
		ABCPlace bf = DeepCopy();
		bf.initBlankMap();
		bf.initMap();
		//無限ループに入る前に、仮置きしないで確定している箇所を処理する
		bf.shaveBlankmapAndSetMap(true);
		// 1階層仮置きして矛盾箇所を削る
		bf.testPutAndShaveBlank(true);
		// この時点で既にMapが完成している場合はループに入る前に処理する
		if(bf.isCorrect() ) {
			if(f==null || !bf.isSameMap(f)){
				return bf.DeepCopy();
			}else {
				return null;
			}
		}

		// メインループ
		ABCPlace rtn = bf.makeAnswerLoop(f);
		if(rtn !=null)return rtn.DeepCopy();
		return null;
	}
	/**
	 * 答えを生成するLoop
	 * @param f
	 * @return
	 */
	private ABCPlace makeAnswerLoop(ABCPlace f){
		while(true) {
			MapPos p = getLowestBlankMap();
			if(p==null)return null;
			ABCPlace backup = DeepCopy();
			// mapに配置（配置出来ない場合は戻してretry）
			if(!setBitToMap(p)) {
				delBlankMapBit(p);
				continue;
			}
			// BlankMapを削り、確定したbitをMapにセットする
			shaveBlankmapAndSetMap(true);
			// 1階層仮置きして矛盾箇所を削る
			testPutAndShaveBlank(true);
			// 矛盾が発生している場合,または2つ目の解探索中に1つ目に到達した場合
			if(isError()|| (f!=null && isSameMap(f))) {
				DeepCopyToThis(backup);
				delBlankMapBit(p);
				continue;
			}
			// 1つ目の解、または2つ目の解に到達している場合
			if(isCorrect() && (f==null || !isSameMap(f)))return this;
			// まだ解に到達していない場合一階層進む
			ABCPlace bf = makeAnswerLoop(f);
			if(bf!=null)return bf;
			// 先の階層でも答えに到達しなかった場合は次に進む
			DeepCopyToThis(backup);
			delBlankMapBit(p);
			continue;
		}
	}
	/**
	 * BlankMapから最も端の空きBitを取得する
	 * @return
	 */
	private MapPos getLowestBlankMap() {
		for(int c=0;c<GAME_COLOR;c++) {
			for(int x=0;x<GAME_SIZE;x++) {
				if(blankMap[c][x]!=0) {
					MapPos rtn =new MapPos();
					rtn.c=c;
					rtn.x=x;
					rtn.bit=Integer.lowestOneBit(blankMap[c][x]);
					return rtn;
				}
			}
		}
		return null;
	}
	/**
	 * 指定されたbitをMapにセットする。Map内の対象色、対象列が0でない場合false
	 * @param p
	 * @return
	 */
	private boolean setBitToMap(MapPos p) {
		if(map[p.c][p.x]!=0)return false;
		map[p.c][p.x]=p.bit;
		return true;
	}
	/**
	 * BlankMapを削り、確定箇所のMapを設定していく（ヒントを参照する場合はhint=true）
	 * @param hint
	 */
	private void shaveBlankmapAndSetMap(boolean hint) {
		while(true)
		{
			ABCPlace bk = DeepCopy();
			if(hint) {
				// ヒントの近くを処理
				shaveBlankmapNearHint();
				// ヒントの遠くを処理
				shaveBlankmapFarHint();
				// ヒント色と異色の障害物の奥を処理
				shaveBlankmapFarBlock();
			}
			// 確定箇所の同ラインを処理
			eraseFixBit();
			// 確定箇所が有ればmapにbitをセットする
			setFixBit();
			if (isSameMapAndBlankMap(bk)) break;
		}
	}
	/**
	 * ヒントの近くの範囲について、他色が入る可能性を除外する
	 */
	private void shaveBlankmapNearHint()
	{

		for (int r = 0; r < hintMaplen[0]; r++){
			for (int n = 0; n < hintMaplen[1]; n++){
				for (int x = 0; x < hintMaplen[2]; x++){
					int hc = hintMap[r][n][x];
					// ヒント未設定の場合はスキップ
					if(hc==-1)continue;

					int bf = blankMap[hc][x]; 		// 010101010
					bf |= map[hc][x];         		// 000000010
					bf = Integer.lowestOneBit(bf);	// 000000010
					bf <<= 1;                  		// 000000100
					bf--;                      		// 000000011
					bf = ~bf;                   	// 111111100
					for (int c = 0; c < GAME_COLOR; c++)
					{
						if (c == hc) continue;
						blankMap[c][x] &= bf;
					}
				}
				for (int c = 0; c < GAME_COLOR; c++)
				{
					blankMap[c] = rotateMap(blankMap[c]);
					map[c] = rotateMap(map[c]);
				}
			}
			if(GAME_TYPE!=3)continue;
			for (int c = 0; c < GAME_COLOR; c++){
				// 左右反転
				blankMap[c] = reverseMap(blankMap[c]);
				map[c] = reverseMap(map[c]);
			}
		}
	}
	/**
	 * ヒントから離れすぎた範囲について可能性を除外する
	 */
	private  void shaveBlankmapFarHint()
	{

		for (int r = 0; r < hintMaplen[0]; r++){
			for (int n = 0; n < hintMaplen[1]; n++){
				for (int x = 0; x < hintMaplen[2]; x++){
					int hc = hintMap[r][n][x];
					// ヒント未設定の場合はスキップ
					if(hc==-1)continue;
					int bf = 0;
					for (int c = 0; c < GAME_COLOR; c++){
						if (c == hc) continue;
						bf |= blankMap[c][x];
						bf |= map[c][x];
					}
					int end = baseMap[x];									// 0001111111
					end++;													// 0010000000
					int bf2 = 0;
					while (end != 0){
						end >>= 1;											// 0001000000
					bf2 |= end;												// 0001100000
					if (Integer.bitCount(bf & bf2) >= GAME_COLOR - 1){
						end <<= 1;											// 0010000000
						end--;												// 0001111111
						blankMap[hc][x] &= end;
						break;
					}
					}
				}
				for (int c = 0; c < GAME_COLOR; c++){
					blankMap[c] = rotateMap(blankMap[c]);
					map[c] = rotateMap(map[c]);
				}
			}
			if(GAME_TYPE !=3)continue;
			for (int c = 0; c < GAME_COLOR; c++){
				// 左右反転
				blankMap[c] = reverseMap(blankMap[c]);
				map[c] = reverseMap(map[c]);
			}
		}
	}
	/**
	 * hintから先に壁となる色が存在するなら、
	 * その壁より奥についてはhintと同色が入る事は無いので、
	 * 遠い箇所について、可能性を除外する
	 */
	private void shaveBlankmapFarBlock()
	{
		for (int r = 0; r < hintMaplen[0]; r++){
			for (int n = 0; n < hintMaplen[1]; n++)
			{
				for (int x = 0; x < hintMaplen[2]; x++)
				{
					int hc = hintMap[r][n][x];
					// ヒント未設定の場合はスキップ
					if(hc==-1)continue;
					int bf = 0;
					// 壁の集合を作成
					for (int c = 0; c < GAME_COLOR; c++)
					{
						if (c == hc) continue;
						bf |= map[c][x];				// 01010100
					}
					// 壁の奥を処理
					bf = Integer.lowestOneBit(bf);		// 00000100
					bf--;                   			// 00000011
					blankMap[hc][x] &= bf;
				}
				for (int c = 0; c < GAME_COLOR; c++)
				{
					blankMap[c] = rotateMap(blankMap[c]);
					map[c] = rotateMap(map[c]);
				}
			}
			if(GAME_TYPE!=3)continue;
			for (int c = 0; c < GAME_COLOR; c++){
				// 左右反転
				blankMap[c] = reverseMap(blankMap[c]);
				map[c] = reverseMap(map[c]);
			}
		}
	}
	private void delBlankMapBit(MapPos p) {
		blankMap[p.c][p.x]&=~p.bit;
	}
	/**
	 * ヒントに基づいた正しい解答が出来ているか確認する
	 * @return
	 */
	private boolean isCorrect() {
		if (bitCountMatrix(map) != GAME_SIZE * GAME_COLOR)return false;
		return checkIntegrity(true);
	}
	/**
	 * ヒントに対して何か矛盾した箇所が存在している場合はTRUE
	 * @return
	 */
	private boolean isError() {
		return !checkIntegrity(true);
	}

	/**
	 * type3の場合ヒントエリアが右回り版と左回り版で重複する為、どちらか一方となるように削る
	 * @return
	 */
	private ABCPlace shaveType3Hints() {
		// 削りパターンの施行開始パターンはランダムに決める
		int start = Rnd.random.nextInt(1<<6);	// 0010101010 rnd=0～3(rnd=2の場合)
		//削りループ前半
		for(int i=start;i<(1<<6);i++) {
			ABCPlace bf=DeepCopy();
			shaveType3HintsSub(bf,i);
			if(bf.isUniqueAnswer()==1)return bf;
		}
		//削りループ後半
		for(int i=0;i<start;i++) {
			ABCPlace bf=DeepCopy();
			shaveType3HintsSub(bf,i);
			if(bf.isUniqueAnswer()==1)return bf;
		}
		// 全ての削りパターンで解一意のQuizが生成不可能だったのでイチからやり直し
		return makeQuiz(GAME_TYPE,GAME_SIZE, GAME_COLOR);
	}
	/**
	 * 指定された削り方でヒントエリアを削る
	 * @param bf
	 * @param i
	 */
	private void shaveType3HintsSub(ABCPlace bf, int i) {
		for (int c = 0; c < 6; c++){
			int bit = (i>>c)&1;
			for (int x = 0; x < GAME_SIZE/2; x++){
				// bitが0の場合は左回転側の、bitが1の場合は右回転側のヒントを削除。
				// ヒントの並び順が左右反転するのでこの処置が要る。
				if(bit==0)bf.hintMap[bit][c][x]=-1;
				if(bit==1) {
					int bf2=7-c;
					if(c==0)bf2=1;
					if(c==1)bf2=0;
					bf.hintMap[bit][bf2][x]=-1;
				}
			}
		}
	}
	/**
	 * 指定された設定のMapを生成する。（解答が一意とは限らない）
	 * @param GAME_TYPE
	 * @param GAME_SIZE
	 * @param GAME_COLOR
	 * @return
	 */
	private ABCPlace makeMap(int GAME_TYPE,int GAME_SIZE, int GAME_COLOR) {
		new ABCPlace();
		init(GAME_TYPE,GAME_SIZE, GAME_COLOR);
		// MAPが完成するまでループ
		while(!isCreatedMap()) {
			// BlankMapからランダムに空きを取得
			MapPos p = getRndBlankBit();
			// BlankMapに空きが皆無の場合最初からやり直し
			if(p==null)return makeMap(GAME_TYPE,GAME_SIZE, GAME_COLOR);

			// BlankMapに空きが存在する場合はMAPに配置する(不可能な場合はBlankMapを削りやり直し)
			if(!setBitToMap(p)) {
				delBlankMapBit(p);
				continue;
			}
			// ヒントを参照せずにBlankMapを削り、確定箇所をMAPに設定する
			shaveBlankmapAndSetMap(false);
			// 仮置きして矛盾する箇所を除外(hintMapは参照しない)
			testPutAndShaveBlank(false);

		}
		// 完成している場合はhintを生成して終了
		makeHint();
		return DeepCopy();
	}
	/**
	 * mapを元にhintmapを生成する
	 */
	private void makeHint()
	{
		for (int r = 0; r < hintMaplen[0]; r++){
			for (int n = 0; n < hintMaplen[1]; n++){
				for (int x = 0; x < hintMaplen[2]; x++){
					hintMap[r][n][x] = getLowestColorFromMapForHint(x);
				}
				for (int c = 0; c < GAME_COLOR; c++)map[c] = rotateMap(map[c]);
			}
			if(GAME_TYPE!=3)continue;
			// 左右反転
			for (int c = 0; c < GAME_COLOR; c++)map[c] = reverseMap(map[c]);
		}
	}
	/**
	 * ヒント生成の為に最も手前の色を取得
	 * @param x
	 * @return
	 */
	private int getLowestColorFromMapForHint(int x) {
		int bf = map[0][x];
		int rtn = 0;
		for (int c = 1; c < GAME_COLOR; c++) {
			if(bf>map[c][x]) {
				bf=map[c][x];
				rtn = c;
			}
		}
		return rtn;
	}
	/**
	 * BlankMapからランダムにbitを取得（存在しない場合はnull）
	 * @return
	 */
	private MapPos getRndBlankBit() {
		MapPos p =  getRndBlankColorLine();
		// 空きが存在しない場合はnullを返して終了
		if(p==null)return null;
		return getRndBlankColorLineBit(p);
	}
	/**
	 * BlankMapから指定した色、指定した列内のbitをランダムに取得する
	 * @param p
	 * @return
	 */
	private MapPos getRndBlankColorLineBit(MapPos p) {			// 例
		int bf = blankMap[p.c][p.x];							// 0010101010 bitcount=4
		int rnd = Rnd.random.nextInt(Integer.bitCount(bf));	// 0010101010 rnd=0～3(rnd=2の場合)
		int cnt = 0;											// 0010101010
		while (rnd != cnt){										// 0010101010
			bf &= ~Integer.lowestOneBit(bf);					// 001010x0x0 lowestを除外
			cnt++;												// 001010x0x0
		}														// 0010100000
		p.bit = Integer.lowestOneBit(bf);						// 0000100000 lowestを取得
		return p;
	}
	/**
	 * blankを含むColor*列をランダムで取得する（blankが存在しない場合はnull）
	 * @return colorと列
	 */
	private MapPos getRndBlankColorLine() {
		// blankが有る列をカウントする
		int cnt = 0;
		for (int c = 0; c < GAME_COLOR; c++)
		{
			for (int x = 0; x < GAME_SIZE; x++)
			{
				if (blankMap[c][x] == 0) continue;
				cnt++;
			}
		}
		if (cnt == 0) return null;
		int rnd = Rnd.random.nextInt(cnt);
		cnt = 0;
		for (int c = 0; c < GAME_COLOR; c++)
		{
			for (int x = 0; x < GAME_SIZE; x++)
			{
				if (blankMap[c][x] == 0) continue;
				if (cnt == rnd)
				{
					MapPos rtn = new MapPos();
					rtn.c = c;
					rtn.x = x;
					return rtn;
				}
				cnt++;
			}
		}
		throw new IllegalArgumentException("有り得ない処理に到達した");
	}
	/**
	 * Map内に全ての色が無矛盾に配置されていればtrue
	 * @return
	 */
	private boolean isCreatedMap() {
		if(bitCountMatrix(map)!=GAME_SIZE * GAME_COLOR)return false;
		if(!checkIntegrity(false))return false;
		return true;
	}
	/**
	 * リスト内の答えを取込み、hintと整合するMAPがリスト内に存在する場合true
	 * @param anotherAnswerList
	 * @return
	 */
	private boolean isInListSameAnswer(ArrayList<ABCPlace> anotherAnswerList) {
		if(anotherAnswerList.isEmpty())return false;
		for(ABCPlace s : anotherAnswerList){
			initMap();
			map = copyMatrix(s.map);
			if(isCorrect())return true;
		}
		return false;
	}
	/**
	 * 1段階だけlowestに仮置きし、矛盾するBlankMapを削る
	 * @param hint
	 */
	private void testPutAndShaveBlank(boolean hint)
	{
		ABCPlace f = DeepCopy();
		for (int c = 0; c < GAME_COLOR; c++)
		{
			for (int x = 0; x < GAME_SIZE; x++)
			{
				if (f.blankMap[c][x] != 0)
				{
					ABCPlace bk = f.DeepCopy();
					int bf = Integer.lowestOneBit(f.blankMap[c][x]);
					// bfの箇所に仮置き
					f.map[c][x] |= bf;
					while (true)
					{
						ABCPlace before = f.DeepCopy();
						f.shaveBlankmapAndSetMap(hint);
						if (f.isSameMapAndBlankMap(before)) break;
					}
					if (f.checkIntegrity(hint) == false)
					{
						f = bk.DeepCopy();
						f.blankMap[c][x] &= ~bf;
					}else
					{
						f = bk.DeepCopy();
					}
				}
			}
		}
		DeepCopyToThis(f);
	}


	/**
	 * 1箇所hintを削れる箇所が有れば削ってtrue、無ければ何も変えずにfalse
	 * @param answer
	 * @param anotherAnswerList
	 * @return
	 */
	private boolean shaveOffHints(ABCPlace answer, ArrayList<ABCPlace> anotherAnswerList) {
		// ヒント総数を取得
		int cntHints = countHints();
		// ヒント総数が0なら異常終了
		if(cntHints==0)throw new IllegalArgumentException("ヒントが皆無になっている");
		int start = Rnd.random.nextInt(cntHints);
		int cnt=0;
		ABCPlace bf = DeepCopy();

		// 指定した位置～[max][max][max]までの間に削除可能なヒントが有るか探索
		cnt = 0;
		for (int r = 0; r < hintMaplen[0]; r++){
			for (int n = 0; n < hintMaplen[1]; n++){
				for (int x = 0; x < hintMaplen[2]; x++){
					if(hintMap[r][n][x]!=-1) {
						if(cnt>=start) {
							int bf2 = hintMap[r][n][x];
							hintMap[r][n][x]=-1;
							if(isUniqueAnswer(answer,anotherAnswerList) == 1) {
								return true;
							}
							hintMap[r][n][x]=bf2;
						}
						cnt++;
					}
				}
			}
		}
		// [0][0][0]～指定した位置の手前までに削除可能なヒントが有るか探索
		cnt = 0;
		for (int r = 0; r < hintMaplen[0]; r++){
			for (int n = 0; n < hintMaplen[1]; n++){
				for (int x = 0; x < hintMaplen[2]; x++){
					if(hintMap[r][n][x]!=-1) {
						if(cnt<start) {
							int bf2 = hintMap[r][n][x];
							hintMap[r][n][x]=-1;
							if(isUniqueAnswer(answer,anotherAnswerList) == 1) {
								return true;
							}
							hintMap[r][n][x]=bf2;
						}
						cnt++;
					}
				}
			}
		}
		// 削除できるヒントが一切ない場合はfalseで終了
		DeepCopyToThis(bf);
		return false;
	}
	private int countHints() {
		int cnt = 0;
		for (int r = 0; r < hintMaplen[0]; r++)
		{
			for (int n = 0; n < hintMaplen[1]; n++)
			{
				for (int x = 0; x < hintMaplen[2]; x++)
				{
					if(hintMap[r][n][x]!=-1)cnt++;
				}
			}
		}
		return cnt;
	}
	/**
	 * 指定のタイプ、サイズ、色数で初期化
	 * @param GAME_TYPE
	 * @param GAME_SIZE
	 * @param GAME_COLOR
	 */
	private void init(int GAME_TYPE,int GAME_SIZE, int GAME_COLOR)
	{
		this.GAME_TYPE = GAME_TYPE;
		this.GAME_SIZE = GAME_SIZE;
		this.GAME_COLOR = GAME_COLOR;
		this.map = new int[GAME_COLOR][GAME_SIZE];
		this.blankMap = new int[GAME_COLOR][GAME_SIZE];
		this.hintMaplen = new int[3];
		if(GAME_TYPE==3) {hintMaplen[0]=2;hintMaplen[1]=6;hintMaplen[2]=GAME_SIZE/2;}
		if(GAME_TYPE==4) {hintMaplen[0]=1;hintMaplen[1]=4;hintMaplen[2]=GAME_SIZE;}
		if(GAME_TYPE==6) {hintMaplen[0]=1;hintMaplen[1]=6;hintMaplen[2]=GAME_SIZE;}
		this.hintMap = new int[hintMaplen[0]][hintMaplen[1]][hintMaplen[2]];
		this.baseMap = new int[GAME_SIZE];
		this.initBaseMap();
		this.initBlankMap();
		this.initMap();
		this.initHintMap();
	}
	private void initHintMap() {
		for (int[][] iss: hintMap)for (int[] is: iss)Arrays.fill(is, -1);
	}
	private void initBaseMap() {
		baseMap = initBlank(GAME_TYPE,GAME_SIZE);
	}
	private void initBlankMap() {
		for (int c = 0; c < GAME_COLOR; c++)blankMap[c] = initBlank(GAME_TYPE,GAME_SIZE);
	}
	private void initMap() {
		for (int[] is: map)Arrays.fill(is, 0);
	}
	/**
	 * 指定サイズでマップを全て111111…で埋める
	 * @param GAME_SIZE
	 * @return
	 */
	private int[] initBlank(int GAME_TYPE,int GAME_SIZE)
	{
		if(GAME_TYPE==3)return initBlankType3(GAME_SIZE);
		if(GAME_TYPE==4)return initBlankType4(GAME_SIZE);
		if(GAME_TYPE==6)return initBlankType6(GAME_SIZE);
		throw new IllegalArgumentException("GAME_TYPE異常");
	}
	private int[] initBlankType6(int GAME_SIZE) {
		int[] bfmap = new int[GAME_SIZE];
		int bf = GAME_SIZE / 2 + 1;
		for (int n = 0; n < GAME_SIZE; n++){
			bfmap[n] = (1 << bf) - 1;
			if(n<GAME_SIZE/2) {	bf++;}else {bf--;}
		}
		return bfmap;
	}
	private int[] initBlankType4(int GAME_SIZE) {
		int[] bfmap = new int[GAME_SIZE];
		for (int n = 0; n < GAME_SIZE; n++)bfmap[n] = (1 << GAME_SIZE) - 1;
		return bfmap;
	}
	private int[] initBlankType3(int GAME_SIZE) {
		int[] bfmap = new int[GAME_SIZE];
		for (int n = 0; n < GAME_SIZE / 2; n++){
			bfmap[n] = (1 << (GAME_SIZE + 1 + n * 2)) - 1;
			bfmap[GAME_SIZE - 1 - n] = (1 << (GAME_SIZE + 1 + n * 2)) - 1;
		}
		return bfmap;
	}
	/**
	 * 整合性を確認する。withHintがtrueならヒントとも整合している必要あり。
	 * 注意：正解パターンに到達しているか否かを判別するメソッドでは無く、
	 * 矛盾しているか否かを確認するのみ。
	 * @param withHint
	 * @return
	 */
	private boolean checkIntegrity(boolean withHint)
	{
		ABCPlace bf = DeepCopy();
		int rotateCnt = 3;
		if(bf.GAME_TYPE==4)rotateCnt=2;
		//確定していないのに置ける場所がなくなっているor確定箇所が2箇所以上ある
		for (int n = 0; n < rotateCnt; n++){
			for (int c = 0; c < bf.GAME_COLOR; c++){
				for (int x = 0; x < bf.GAME_SIZE; x++){
					if (bf.blankMap[c][x] == 0 && bf.map[c][x] == 0)return false;
					if(Integer.bitCount(bf.map[c][x]) >= 2)return false;
				}
			}
			for (int c = 0; c < bf.GAME_COLOR; c++) {
				bf.blankMap[c] = rotateMap(bf.blankMap[c]);
				bf.map[c] = rotateMap(bf.map[c]);
			}
		}
		// 同一箇所に2色以上確定色があってはいけない
		for (int c = 0; c < bf.GAME_COLOR; c++)
		{
			for (int x = 0; x < bf.GAME_SIZE; x++)
			{
				if (bf.map[c][x] !=0)
				{
					for (int c2 = c+1; c2 < bf.GAME_COLOR; c2++)
					{
						if (c == c2) continue;
						if ((bf.map[c][x]&bf.map[c2][x]) != 0) return false;
					}
				}
			}
		}
		if (!withHint)return true;
		bf = DeepCopy();
		// ヒントの整合性チェック
		for (int r = 0; r < bf.hintMaplen[0]; r++){
			for (int n = 0; n < bf.hintMaplen[1]; n++)
			{
				for (int x = 0; x < bf.hintMaplen[2]; x++)
				{
					//もしヒント色と同色が確定しているなら、その手前に別の色が確定していてはいけない
					//もしヒント色と同色が未確定なら、未確定マップのlowより手前に、別の色が確定していてはいけない
					int hc = bf.hintMap[r][n][x];
					// ヒント未設定の場合はスキップ
					if(hc==-1)continue;
					int i = bf.map[hc][x];
					i |= bf.blankMap[hc][x];
					i = Integer.lowestOneBit(i);
					for (int c = 0; c < bf.GAME_COLOR; c++)
					{
						if (c == hc) continue;
						if (bf.map[c][x] != 0 && i >= bf.map[c][x]) return false;
					}
				}
				for (int c = 0; c < bf.GAME_COLOR; c++)
				{
					bf.blankMap[c] = rotateMap(bf.blankMap[c]);
					bf.map[c] = rotateMap(bf.map[c]);
				}
			}
			if(bf.GAME_TYPE!=3)continue;
			for (int c = 0; c < bf.GAME_COLOR; c++){
				// 左右反転
				bf.blankMap[c] = reverseMap(bf.blankMap[c]);
				bf.map[c] = reverseMap(bf.map[c]);
			}
		}
		return true;
	}
	private boolean isSameMap(ABCPlace f)
	{
		for (int c = 0; c < this.GAME_COLOR; c++)
		{
			for (int x = 0; x < this.GAME_SIZE; x++)
			{
				if (f.map[c][x] != this.map[c][x]) return false;
			}
		}
		return true;
	}
	private boolean isSameMapAndBlankMap(ABCPlace f)
	{
		for (int c = 0; c < this.GAME_COLOR; c++)
		{
			for (int x = 0; x < this.GAME_SIZE; x++)
			{
				if (f.blankMap[c][x] != this.blankMap[c][x]) return false;
				if (f.map[c][x] != this.map[c][x]) return false;
			}
		}
		return true;
	}

	/**
	 * blankmapが1bitしか残っていない場合、mapに配置する。(blankMap状態は変更しない)
	 */
	private void setFixBit()
	{
		if(GAME_TYPE==3)setFixBitType3();
		if(GAME_TYPE==6)setFixBitType6();
		if(GAME_TYPE==4)setFixBitType4();
	}

	private void setFixBitType4() {
		for (int n = 0; n < 4; n++)
		{
			for (int c = 0; c < GAME_COLOR; c++)
			{
				for (int x = 0; x <= GAME_SIZE / 2; x++)
				{
					if (Integer.bitCount(blankMap[c][x]) == 1 && map[c][x] == 0)
					{
						map[c][x] = blankMap[c][x];
					}
				}
			}
			for (int c = 0; c < GAME_COLOR; c++)
			{
				blankMap[c] = rotateMap(blankMap[c]);
				map[c] = rotateMap(map[c]);
			}
		}
	}
	private void setFixBitType6() {
		for (int n = 0; n < 6; n++)
		{
			for (int c = 0; c < GAME_COLOR; c++)
			{
				for (int x = 0; x <= GAME_SIZE / 2; x++)
				{
					if (Integer.bitCount(blankMap[c][x]) == 1 && map[c][x] == 0)
					{
						map[c][x] = blankMap[c][x];
					}
				}
			}
			for (int c = 0; c < GAME_COLOR; c++)
			{
				blankMap[c] = rotateMap(blankMap[c]);
				map[c] = rotateMap(map[c]);
			}
		}
	}
	private void setFixBitType3() {
		for (int n = 0; n < 6; n++)
		{
			for (int c = 0; c < GAME_COLOR; c++)
			{
				for (int x = 0; x < GAME_SIZE / 2; x++)
				{
					if (Integer.bitCount(blankMap[c][x]) == 1&& map[c][x] == 0)
					{
						map[c][x] = blankMap[c][x];
					}
				}
			}
			for (int c = 0; c < GAME_COLOR; c++)
			{
				blankMap[c] = rotateMap(blankMap[c]);
				map[c] = rotateMap(map[c]);
			}
		}
	}
	/**
	 * 配置する色が確定している場合
	 * 同色のblankmapは列単位で0化
	 * 他色のblankmapは対象bitのみを0化する
	 */
	private void eraseFixBit()
	{
		int xlen = GAME_SIZE / 2;
		if(GAME_TYPE==6)xlen++;
		for (int n = 0; n < hintMaplen[1]; n++)
		{
			for (int c = 0; c < GAME_COLOR; c++)
			{
				for (int x = 0; x < xlen; x++)
				{
					if (Integer.bitCount(map[c][x]) == 1)
					{
						int bf = map[c][x];
						// 同色の同列は0に
						blankMap[c][x] = 0;
						// 全ての色のblankmapから削除する（点の処理なので全角度で行う必要は無い。
						// x部分がGAME_SIZEの半分以下なので、TYPE3,6なら%3=0、TYPE4なら%2=0のみで行う
						if (GAME_TYPE == 3 && n % 3 == 0) for (int m = 0; m < GAME_COLOR; m++) blankMap[m][x] &= ~bf;
						if (GAME_TYPE == 6 && n % 3 == 0) for (int m = 0; m < GAME_COLOR; m++) blankMap[m][x] &= ~bf;
						if (GAME_TYPE == 4 && n % 2 == 0) for (int m = 0; m < GAME_COLOR; m++) blankMap[m][x] &= ~bf;
					}
				}
			}
			for (int c = 0; c < GAME_COLOR; c++)
			{
				blankMap[c] = rotateMap(blankMap[c]);
				map[c] = rotateMap(map[c]);
			}
		}
	}

	/**
	 * mapを回転する
	 * @param m
	 * @return
	 */
	private int[] rotateMap(int[] m)
	{
		if(GAME_TYPE==3)return rotateMapType3(m);
		if(GAME_TYPE==4)return rotateMapType4(m);
		if(GAME_TYPE==6)return rotateMapType6(m);
		throw new IllegalArgumentException("GAME_TYPE異常");
	}

	private int[] rotateMapType3(int[] m) {
		int mlen = m.length;
		int[] map = new int[mlen];
		map=m.clone();
		//bfMapを作成し0で初期化
		int[] bfMap = new int[mlen];
		Arrays.fill(bfMap,0);
		// 変換元MAPの右側半分の範囲は、前処理としてズラしてから進める
		int cnt = 1;
		for (int i = mlen / 2; i < mlen; i++)
		{
			map[i] <<= cnt;
			cnt += 2;
		}
		//11,11,…,11で区切り、bfに左側から入れていく
		for (int i = 0; i < mlen; i++)
		{
			for (int n = 0; n < mlen; n++) bfMap[n] <<= 2;
			cnt = 0;
			while (map[i] > 0)
			{
				bfMap[cnt] |= (map[i] & 3);
				map[i] >>= 2;
			cnt++;
			}
		}
		// bfMapの半分より手前側は元MAPの補正分ズレてしまうので補正する
		cnt = (mlen / 2 - 1) * 2 + 1;
		for (int i = 0; i < mlen / 2; i++)
		{
			bfMap[i] >>= cnt;
		cnt -= 2;
		}
		return bfMap;
	}
	private int[] rotateMapType4(int[] m) {
		int mlen = m.length;
		int[] map = new int[mlen];
		map=m.clone();
		//bfMapを作成し0で初期化
		int[] bfMap = new int[mlen];
		Arrays.fill(bfMap,0);
		for (int i = 0; i < mlen; i++)
		{
			int bf = 0;
			while(bf<mlen){
				bfMap[bf]<<=1;
				bfMap[bf]|=map[i]&1;
				map[i]>>=1;
				bf++;
			}
		}
		return bfMap;
	}
	private int[] rotateMapType6(int[] m) {
		int mlen = m.length;
		int[] map = new int[mlen];
		map=m.clone();
		//bfMapを作成し0で初期化
		int[] bfMap = new int[mlen];
		Arrays.fill(bfMap,0);
		// 変換元MAPの右側半分の範囲は、前処理としてズラしてから進める
		int cnt = 1;
		for (int i = mlen / 2 + 1; i < mlen; i++){
			map[i] <<= cnt;
			cnt ++;
		}
		//1,1,…,1で区切り、bfに左側から入れていく
		for (int i = 0; i < mlen; i++){
			for (int n = 0; n < mlen; n++) bfMap[n] <<= 1;
			cnt = 0;
			while (map[i] > 0){
				bfMap[cnt] |= (map[i] & 1);
				map[i] >>= 1;
			cnt++;
			}
		}
		// bfMapの半分より手前側は元MAPの補正分ズレてしまうので補正する
		cnt = mlen / 2;
		for (int i = 0; i < mlen / 2; i++){
			bfMap[i] >>= cnt;
		cnt--;
		}
		return bfMap;
	}
	/**
	 * 左右反転
	 * @param is
	 * @return
	 */
	private int[] reverseMap(int[] is) {
		int islen=is.length;
		int[] bf=new int[islen];
		for(int i=0;i<islen;i++)bf[i]=is[islen-i-1];
		return bf;
	}
	/**
	 * int[][]のbitをカウントする
	 * @return
	 */
	private int bitCountMatrix(int[][] map)
	{
		int maplen0=map.length;
		int maplen1=map[0].length;
		int bf = 0;
		for (int x = 0; x < maplen0; x++) {
			for (int y = 0; y < maplen1; y++) {
				bf += Integer.bitCount(map[x][y]);
			}
		}
		return bf;
	}
	private int[][] copyMatrix(int[][] map) {
		int maplen0=map.length;
		int maplen1=map[0].length;
		int[][] rtn = new int[maplen0][maplen1];
		for(int x = 0;x<maplen0;x++)rtn[x]=map[x].clone();
		return rtn;
	}

}
class MapPos {
	int x;
	int c;
	int bit;
}
class Rnd {
	public static final Random random = new Random();
}



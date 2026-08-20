package com.spoiledmilk.spritebaker;

/** Independent source-browser and destination-cell selection state for the editor. */
public final class DirectionAssignmentSelection {
    private int sourceDirection;
    private int destinationRow;
    private int destinationColumn;

    public int sourceDirection(){return sourceDirection;}
    public int destinationRow(){return destinationRow;}
    public int destinationColumn(){return destinationColumn;}

    /** Browsing a source view never changes the selected sheet destination. */
    public void browseSource(int direction){sourceDirection=SheetDirection.checked(direction);}

    /** Selecting a cell also browses its matching view as an initial convenience. */
    public void selectDestination(int row,int column){
        if(row<0||row>=TargetSheet.ROWS)throw new IllegalArgumentException("row outside target sheet: "+row);
        destinationRow=row;destinationColumn=SheetDirection.checked(column);sourceDirection=column;
    }

    public String sourceLabel(){return "Browsing source: "+SheetDirection.label(sourceDirection);}
    public String destinationLabel(){return "Destination: "+TargetSheet.ROW_LABELS[destinationRow]+" / "+SheetDirection.label(destinationColumn);}
    /** Direction for source-browser imagery; the final preview uses {@link FinalPreviewModel}. */
    public int previewDirection(TargetSheet sheet,boolean sourceAlternative){
        if(sheet==null)throw new NullPointerException("sheet");
        return sourceAlternative?sourceDirection:sheet.effectiveSourceDirection(destinationRow,destinationColumn);
    }
}
